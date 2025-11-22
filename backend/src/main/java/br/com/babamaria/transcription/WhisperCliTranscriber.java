package br.com.babamaria.transcription;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

public class WhisperCliTranscriber {
    // Custom npx path from config (set in application.properties as app.npxPath)
    @Value("${app.npxPath:}")
    private static String customNpxPath = System.getProperty("app.npxPath", "");

    /**
     * Executa o `whisper-cli` via `npx` com os argumentos fornecidos e retorna a saída combinada.
     * Exemplo de uso (manual):
     *   WhisperCliTranscriber.transcribeWithArgs("audio.mp3", "--model", "small");
     * Ou chamar o script npm direto: `npm run transcribe -- audio.mp3 --model small`.
     *
     * Observações:
     * - Requer Node.js / npx instalado no ambiente de execução.
     * - O método apenas executa o CLI e captura stdout/stderr como string.
     */
    /**
     * Converte um arquivo .webm para .wav usando ffmpeg.
     * @param inputWebm Caminho do arquivo .webm de entrada
     * @return Caminho do arquivo .wav gerado
     * @throws Exception se a conversão falhar
     */
    public static String ConvertAudioFile(String inputWebm) throws Exception {
        // inputWebm is expected to be a path (absolute or relative) to the saved upload
        Path inputPath = Paths.get(inputWebm);
        // normalize to absolute path to avoid surprises
        if (!inputPath.isAbsolute()) {
            inputPath = inputPath.toAbsolutePath().normalize();
        }

        // If input has no parent (just a file name), use working directory
        Path parent = inputPath.getParent();
        Path outputDir = (parent != null) ? parent : Paths.get(System.getProperty("user.dir")).toAbsolutePath();

        String fileName = inputPath.getFileName().toString();
        String baseName = fileName.replaceFirst("(?i)\\.webm$", "");
        Path outputPath = outputDir.resolve(baseName + ".wav");

        // Ensure output directory exists
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }

        // Determine ffmpeg executable: system property `app.ffmpegPath` or env `FFMPEG_PATH` can override
        String ffmpegCandidate = System.getProperty("app.ffmpegPath");
        if (ffmpegCandidate == null || ffmpegCandidate.trim().isEmpty()) {
            ffmpegCandidate = System.getenv("FFMPEG_PATH");
        }
        String ffmpegCmd = (ffmpegCandidate != null && !ffmpegCandidate.trim().isEmpty()) ? ffmpegCandidate.trim() : "ffmpeg";

        List<String> cmd = new ArrayList<>();
        cmd.add(ffmpegCmd);
        cmd.add("-y"); // overwrite if exists
        cmd.add("-i");
        cmd.add(inputPath.toString());
        cmd.add(outputPath.toString());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(new File(System.getProperty("user.dir")));
        pb.redirectErrorStream(true);

        Process p;
        try {
            p = pb.start();
        } catch (java.io.IOException e) {
            String hint = "Failed to start '" + ffmpegCmd + "'. Ensure ffmpeg is installed and available on PATH, or set system property 'app.ffmpegPath' or environment variable 'FFMPEG_PATH' with full path to ffmpeg executable.\nOriginal error: " + e.getMessage();
            throw new java.io.IOException(hint, e);
        }
        StringBuilder out = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                out.append(line).append(System.lineSeparator());
            }
        }
        int exit = p.waitFor();
        if (exit != 0) {
            throw new RuntimeException("ffmpeg exited with code " + exit + ":\n" + out.toString());
        }
        return outputPath.toString();
    }

    public static String transcribeWithArgs(MultipartFile file, String... extraArgs) throws Exception {
        List<String> cmd = new ArrayList<>();
        String npxCmd;
        if (customNpxPath != null && !customNpxPath.trim().isEmpty()) {
            npxCmd = customNpxPath.trim();
        } else {
            npxCmd = "npx";
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                npxCmd = "npx.cmd";
            }
        }
        cmd.add(npxCmd);
        cmd.add("whisper-cli");
        cmd.add("smart");
        cmd.add("transcribe");


        Path uploadPath = Paths.get("uploads");

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String original = file.getOriginalFilename();
        String safeName;
        if (original == null || original.trim().isEmpty()) {
            safeName = UUID.randomUUID().toString() + ".webm";
        } else {
            safeName = System.currentTimeMillis() + "-" + Paths.get(original).getFileName().toString();
        }

        Path filePath = uploadPath.resolve(safeName);
        Files.write(filePath, file.getBytes());

        // Convert the saved upload to wav (this will validate the file)
        String converted = ConvertAudioFile(filePath.toString());

        // Use forward slashes for CLI compatibility
        String cliFilePath = converted.replace('\\', '/');
        cmd.add(cliFilePath);

        extraArgs = new String[] { "--model", "C:/Dados/Desenvolvimentos/BabaMaria/backend/whisper.cpp/models/ggml-base.bin", "--language", "pt" };
        
        if (extraArgs != null) {
            Collections.addAll(cmd, extraArgs);
        }

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(new File(System.getProperty("user.dir")));
        pb.redirectErrorStream(true);

        Process p;
        try {
            p = pb.start();
        } catch (java.io.IOException e) {
            String hint = "Failed to start '" + npxCmd + "'. Ensure Node.js and npm (npx) are installed and available on PATH. "
                    + "On Windows install from https://nodejs.org/ and restart the service/console.\nOriginal error: " + e.getMessage();
            throw new java.io.IOException(hint, e);
        }

        StringBuilder out = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                out.append(line).append(System.lineSeparator());
            }
        }

        boolean finished = p.waitFor(Duration.ofMinutes(15).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        if (!finished) {
            p.destroyForcibly();
            throw new RuntimeException("whisper-cli timed out");
        }

        int exit = p.exitValue();
        if (exit != 0) {
            throw new RuntimeException("whisper-cli exited with code " + exit + ":\n" + out.toString());
        }



        String log = out.toString();

        // Regex para capturar o conteúdo de "text"
        Pattern pattern = Pattern.compile("\"text\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(log);
        String text = "";

        // Se houver correspondência
        if (matcher.find()) {
            text = matcher.group(1);
        }

        return text;
    }
}
