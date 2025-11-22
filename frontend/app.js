let chunks = [];
let rec;
let controlRecord = true;

const btnGravar = document.getElementById("btngravar");
const btnUpload = document.getElementById("uploadBtn");
const resultBox = document.getElementById("resultBox");
const divTranscricao = document.getElementById("divTranscricao");

const transcricao = document.getElementById("transcricaotxt");
const loadingOverlay = document.getElementById("loadingOverlay");

btnGravar.onclick = async () => {
    if (controlRecord) {
        chunks = [];
        let stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        rec = new MediaRecorder(stream);
        rec.ondataavailable = e => chunks.push(e.data);
        rec.start();
        btnGravar.innerText = "Gravando...clique para parar";
        controlRecord = false;
    } else {
        rec.stop();
        btnGravar.innerText = "Áudio Gravado";
    }
};

btnUpload.onclick = async () => {
    if (chunks.length === 0) {
        alert("Nenhum áudio gravado!");
        return;
    }

    loadingOverlay.style.display = "flex"; // mostra loading

    let blob = new Blob(chunks, { type: "audio/webm" });
    let file = new File([blob], "audio.webm");
    let form = new FormData();
    form.append("file", file);

    try {
        let res = await fetch("http://localhost:8080/api/transcriptions/upload", {
            method: "POST",
            body: form
        });

        let data = await res.json();

        // Atualiza a transcrição
        transcricao.value = data.transcricaotxt;

        // Limpa resultados anteriores
        resultBox.innerHTML = "";

        // Mostra resultado
        const div = document.createElement("div");
        div.className = data.secureAudio === "Áudio confiável!" ? "sucesso" : "perigo";
        
		const paragrafo = document.createElement("p");
		paragrafo.innerHTML = "<b>Transcrição</b>: " + data.transcricaotxt;
		divTranscricao.appendChild(paragrafo);
		
		divTranscricao.style.display = "block";
		resultBox.style.display = "block";
		
		if(data.secureAudio) {
			div.innerHTML = `<p>Seu áudio é seguro.</p>`;
			div.style.backgroundColor = "#179217";
		} else {
			div.innerHTML = `<p>CUIDADO! Audio pode conter golpe.</p>`;
			div.style.backgroundColor = "#a51919";
		}
        
		resultBox.appendChild(div);

    } catch (err) {
        console.error(err);
        alert("Erro ao processar áudio!");
    } finally {
        loadingOverlay.style.display = "none"; // esconde loading
    }
};
