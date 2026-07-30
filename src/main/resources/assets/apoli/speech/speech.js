(function () {
    const LANGUAGES = [
        ["en-US", "English (US)"],
        ["en-GB", "English (UK)"],
        ["es-ES", "Espanol (Espana)"],
        ["es-MX", "Espanol (Mexico)"],
        ["fr-FR", "Francais"],
        ["de-DE", "Deutsch"],
        ["it-IT", "Italiano"],
        ["pt-BR", "Portugues (Brasil)"],
        ["pt-PT", "Portugues (Portugal)"],
        ["nl-NL", "Nederlands"],
        ["pl-PL", "Polski"],
        ["ru-RU", "Russkij"],
        ["uk-UA", "Ukrainska"],
        ["cs-CZ", "Cestina"],
        ["sv-SE", "Svenska"],
        ["nb-NO", "Norsk"],
        ["da-DK", "Dansk"],
        ["fi-FI", "Suomi"],
        ["tr-TR", "Turkce"],
        ["el-GR", "Ellinika"],
        ["ro-RO", "Romana"],
        ["hu-HU", "Magyar"],
        ["ar-SA", "Arabic"],
        ["he-IL", "Hebrew"],
        ["hi-IN", "Hindi"],
        ["id-ID", "Bahasa Indonesia"],
        ["th-TH", "Thai"],
        ["vi-VN", "Tieng Viet"],
        ["ja-JP", "Japanese"],
        ["ko-KR", "Korean"],
        ["zh-CN", "Chinese (Simplified)"],
        ["zh-TW", "Chinese (Traditional)"]
    ];

    const params = new URLSearchParams(window.location.search);
    const requested = params.get("lang") || navigator.language || "en-US";
    const Recognition = window.SpeechRecognition || window.webkitSpeechRecognition;

    const statusEl = document.getElementById("status");
    const transcriptEl = document.getElementById("transcript");
    const select = document.getElementById("langSelect");

    function pickInitial(tag) {
        for (let i = 0; i < LANGUAGES.length; i++) {
            if (LANGUAGES[i][0].toLowerCase() === tag.toLowerCase()) {
                return LANGUAGES[i][0];
            }
        }
        const base = tag.split("-")[0].toLowerCase();
        for (let i = 0; i < LANGUAGES.length; i++) {
            if (LANGUAGES[i][0].split("-")[0].toLowerCase() === base) {
                return LANGUAGES[i][0];
            }
        }
        return "en-US";
    }

    let currentLang = pickInitial(requested);

    for (let i = 0; i < LANGUAGES.length; i++) {
        const opt = document.createElement("option");
        opt.value = LANGUAGES[i][0];
        opt.textContent = LANGUAGES[i][1] + " (" + LANGUAGES[i][0] + ")";
        if (LANGUAGES[i][0] === currentLang) {
            opt.selected = true;
        }
        select.appendChild(opt);
    }

    function post(text, isFinal) {
        fetch("/transcript", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ text: text, language: currentLang, final: !!isFinal })
        }).catch(function () {});
    }

    if (!Recognition) {
        statusEl.innerText = "This browser does not support the Web Speech API. Falling back to offline recognition in-game if configured.";
        fetch("/nosupport", { method: "POST" }).catch(function () {});
        return;
    }

    const recognition = new Recognition();
    recognition.lang = currentLang;
    recognition.continuous = true;
    recognition.interimResults = true;

    let stopped = false;
    let networkErrors = 0;
    let lastPosted = "";

    select.addEventListener("change", function () {
        currentLang = select.value;
        networkErrors = 0;
        lastPosted = "";
        recognition.lang = currentLang;
        statusEl.innerText = "Switching to " + currentLang + "…";
        try { recognition.stop(); } catch (e) {}
    });

    recognition.onresult = function (event) {
        for (let i = event.resultIndex; i < event.results.length; i++) {
            const result = event.results[i];
            const text = result[0].transcript.trim();
            transcriptEl.innerText = text;
            if (text.length > 0 && text !== lastPosted) {
                lastPosted = text;
                post(text, result.isFinal);
            }
            if (result.isFinal) {
                lastPosted = "";
            }
        }
    };

    recognition.onend = function () {
        if (!stopped) {
            try { recognition.start(); } catch (e) {}
        }
    };

    recognition.onerror = function (event) {
        if (event.error === "not-allowed" || event.error === "service-not-allowed") {
            stopped = true;
            statusEl.innerText = "Microphone permission denied. Allow the mic and reload.";
        } else if (event.error === "network") {
            networkErrors++;
            if (networkErrors >= 2) {
                stopped = true;
                statusEl.innerText = "The browser's online speech service is unreachable (common on Linux). Switching to in-game offline recognition (Vosk).";
                fetch("/nosupport", { method: "POST" }).catch(function () {});
            } else {
                statusEl.innerText = "Online speech service unreachable (network) - retrying...";
            }
        } else {
            statusEl.innerText = "Recognition error: " + event.error + " (retrying)";
        }
    };

    try {
        recognition.start();
        statusEl.innerText = "Listening…";
    } catch (e) {
        statusEl.innerText = "Could not start recognition: " + e.message;
    }

    try {
        const audioContext = new (window.AudioContext || window.webkitAudioContext)();
        const oscillator = audioContext.createOscillator();
        const gain = audioContext.createGain();
        gain.gain.value = 0.0001;
        oscillator.connect(gain);
        gain.connect(audioContext.destination);
        oscillator.start();
        document.addEventListener("click", function resume() {
            audioContext.resume();
            document.removeEventListener("click", resume);
        });
    } catch (e) {}
})();
