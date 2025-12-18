# 📣 Calliope Kotlin — Audio Advertisement Generation POC

[![Project Overview](https://i.sstatic.net/Vp2cE.png)](https://youtu.be/a-tQtecGbBE)

Calliope Kotlin is a technical proof of concept (POC) showcasing an end-to-end system for automated generation of multimedia advertisement assets.

The goal of this POC is to demonstrate how AI models can be orchestrated together to automatically produce rich ad content — including text copy, voice-overs, music, and visual banners — from a simple client prompt.

## 🚀 Core Capabilities

#### 1. Text Advertisement Generation

- Based on a client’s prompt, the system generates advertising text content.
- This typically includes engaging headlines, body copy, and any other required messaging.

#### 2. Audio Voice-Over Synthesis

- The generated text is converted into speech using cloud text-to-speech services.
- The system can leverage major providers — AWS Polly, Google Cloud Text-to-Speech, Azure TTS — with support for:
- - Multiple languages
- - Different voices (gender, style, expressiveness)
- - Fine control over speaking rate and tone

#### 3. Music Generation (Mood-Driven)

- A background music track is produced in a mood or style requested by the client.
- This POC goes beyond simple TTS and uses open-source generative music models such as:
- - facebook/musicgen-stereo-small
- - Models from the Audiocraft family by Facebook Research (for music generation)
- These models are capable of producing musical content directly from text or embeddings, enabling expressive soundtracks that fit the advert’s tone.
GitHub

#### 4. Banner (Image) Generation

- A visual advertisement (banner) is also created from the client’s prompt using image generation models.
- The output can include logos, illustration styles, branding visuals, or other imagery relevant to the campaign.

## 🎯 What This POC Demonstrates

The calliope_kotlin POC is not just a collection of isolated AI demos — it is a coordinated pipeline showing how different AI modalities can be integrated into a unified creative workflow:

| Stage | Input | AI Component | Output |
| --- | --- | --- | --- |
| 1 | Client prompt | LLM text generation | Ad copy |
| 2 | Generated text | Cloud TTS (AWS / Google / Azure) | Voice-over audio |
| 3 | Mood + prompt | Open-source music generative models | Background music |
| 4 | Client prompt | Vision-capable generator | Banner image |

### 🎥 Demonstration

There is a short demo video that walks through the POC in action — showing how each component (text, speech, music and imagery) is generated and combined into an ad-ready multimedia asset:
👉 https://youtu.be/a-tQtecGbBE
 
YouTube

### 🧠 Technical Highlights

- The backend is implemented in Kotlin, with modular APIs for each type of content generation.
- The system illustrates how cloud services (for TTS) and open-source models (for music and visual generation) can be composed into a single workflow.
- Music generation uses inference from models developed by Meta/Facebook (via musicgen and audiocraft), demonstrating text-conditioned audio creation capabilities.
GitHub

### 🔜 Future Directions

This POC can be extended to:

- Add brand style transfer for audio and visuals
- Support multi-language advertising
- Integrate campaign automation tools
- Deploy as a production-ready service
