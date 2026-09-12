//Get references to the UI elements.
const recordBtn = document.getElementById('recordBtn');
const statusEl = document.getElementById('status');
const transcriptEl = document.getElementById('transcript');

let mediaRecorder;
let audioChunks = [];
let isRecording = false;

//Toggle between starting and stopping the recording.
recordBtn.addEventListener('click', () => {
  isRecording ? stopRecording() : startRecording();
});

async function startRecording() {
  try {
    //Request to use user's microphone
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true });

    //Create a recorder for the microphone stream
    mediaRecorder = new MediaRecorder(stream);
    audioChunks = [];

    //Collect audio data
    mediaRecorder.ondataavailable = (event) => {
      if (event.data.size > 0) audioChunks.push(event.data);
    };

    //Process the audio when recording stops
    mediaRecorder.onstop = handleRecordingStopped;

    //Start recording & update the UI
    mediaRecorder.start();
    isRecording = true;
    setState('recording');
  } catch (err) {
    //Display error if microphone access fails
    statusEl.textContent = `Microphone access failed: ${err.message}`;
    setState('idle');
  }
}

function stopRecording() {
  //Stop recording and release the microphone
  mediaRecorder.stop();
  mediaRecorder.stream.getTracks().forEach((track) => track.stop());
  isRecording = false;
}

async function handleRecordingStopped() {
  //Show that the audio is being processed
  setState('processing');

  //Combine the recorded chunks into one audio blob
  const audioBlob = new Blob(audioChunks, { type: mediaRecorder.mimeType });

  try {
    //Send the audio to the server for transcription
    const transcript = await uploadAudio(audioBlob);

    //Display the transcript
    transcriptEl.textContent = transcript;
    statusEl.textContent = 'Ready to record.';
  } catch (err) {
    //Display error if transcription fails.
    statusEl.textContent = `Transcription failed: ${err.message}`;
  } finally {
    //Return the UI to the idle state
    setState('idle');
  }
}

async function uploadAudio(blob) {
  //Send the recorded audio to the transcription API
  const response = await fetch('/api/v1/speech/transcribe', {
    method: 'POST',
    headers: { 'Content-Type': blob.type },
    body: blob
  });

  //Convert server errors into JavaScript errors
  if (!response.ok) {
    const errorBody = await response.json().catch(() => null);
    throw new Error(errorBody?.message ?? `Server returned ${response.status}`);
  }

  //Read the transcript from the successful response
  const data = await response.json();
  return data.transcript;
}

function setState(state) {
  //Reset the button styling before applying the new state
  recordBtn.classList.remove('recording', 'processing');
  recordBtn.disabled = false;

  if (state === 'recording') {
    //Update the UI while recording.
    recordBtn.textContent = '⏹ Stop Recording';
    recordBtn.classList.add('recording');
    recordBtn.setAttribute('aria-pressed', 'true');
    statusEl.textContent = 'Recording... speak now.';
  } else if (state === 'processing') {
    //Disable the button while transcription is running
    recordBtn.disabled = true;
    recordBtn.classList.add('processing');
    statusEl.textContent = 'Processing transcription...';
  } else {
    //Reset the UI when ready to record again
    recordBtn.textContent = 'Start Recording';
    recordBtn.setAttribute('aria-pressed', 'false');
  }
}

