//Get references to the UI elements.
const recordBtn = document.getElementById('recordBtn');
const pauseResumeBtn = document.getElementById('pauseResumeBtn');
const finishBtn = document.getElementById('finishBtn');
const statusEl = document.getElementById('status');
const transcriptEl = document.getElementById('transcript');
const timerEl = document.getElementById('timer');

let mediaRecorder;
let audioChunks = [];

let recordingStartTime;
let elapsedBeforePauseMs = 0;
let timerIntervalId;

//Toggle between starting, pausing and stopping the recording
recordBtn.addEventListener('click', startRecording);
pauseResumeBtn.addEventListener('click', () => {
  //Check the current recorder state to decide whether to pause or resume
  if (mediaRecorder.state === 'recording') {
    pauseRecording();
  } else if (mediaRecorder.state === 'paused') {
    resumeRecording();
  }
});
finishBtn.addEventListener('click', finishRecording);


//Starts a new microphone recording and prepares the audio data
async function startRecording() {
  try {
    //Request permission to use the user's microphone
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true });

    //Create a recorder using the microphone stream
    mediaRecorder = new MediaRecorder(stream);
    audioChunks = [];

    //Store each piece of recorded audio
    mediaRecorder.ondataavailable = (event) => {
      if (event.data.size > 0) audioChunks.push(event.data);
    };

    //Process the audio after the recording has stopped
    mediaRecorder.onstop = handleFinished;

    //Start recording and the timer
    mediaRecorder.start();
    setState('recording');
    startTimer();

  } catch (err) {
    //Show an error if microphone access is denied or fails
    statusEl.textContent = `Microphone access failed: ${err.message}`;
    setState('idle');
  }
}


//Pauses the current recording and stops the timer temporarily
function pauseRecording() {
  mediaRecorder.pause();
  setState('paused');
  pauseTimer();
}


//Resumes a paused recording and starts the timer again
function resumeRecording() {
  mediaRecorder.resume();
  setState('recording');
  resumeTimer();
}


//Stops the recording, releases the microphone and stops the timer
function finishRecording() {
  if (!mediaRecorder || mediaRecorder.state === 'inactive') return;

  //Stop the recorder. This works from both recording and paused states
  mediaRecorder.stop();

  //Release the microphone so the browser no longer uses it
  mediaRecorder.stream.getTracks().forEach((track) => track.stop());

  //Stop updating the timer
  stopTimer();
}


//Combines the recorded audio and sends it to the server for transcription
async function handleFinished() {
  //Tell user that the audio is being processed
  setState('processing');

  //Combine all recorded audio chunks into one audio file
  const audioBlob = new Blob(audioChunks, {
    type: mediaRecorder.mimeType
  });

  try {
    //Upload the audio to the backend for transcription
    const transcript = await uploadAudio(audioBlob);

    //Display the returned transcript on the page
    transcriptEl.textContent = transcript;
    statusEl.textContent = 'Ready to record.';

  } catch (err) {
    //Show an error if the transcription request fails
    statusEl.textContent = `Transcription failed: ${err.message}`;

  } finally {
    //Return the interface to the ready state
    setState('idle');
  }
}


//Sends the recorded audio to the backend transcription endpoint
async function uploadAudio(blob) {
  //Send the audio file to the server using a POST request
  const response = await fetch('/api/v1/speech/transcribe', {
    method: 'POST',
    headers: { 'Content-Type': blob.type },
    body: blob
  });

  //Convert an unsuccessful server response into a JavaScript error
  if (!response.ok) {
    const errorBody = await response.json().catch(() => null);
    throw new Error(
      errorBody?.message ?? `Server returned ${response.status}`
    );
  }

  //Read the JSON response and return the transcript text
  const data = await response.json();
  return data.transcript;
}


//Updates the buttons and status message based on the current application state
function setState(state) {
  //Hide all buttons before showing only the ones needed for this state
  recordBtn.hidden = true;
  pauseResumeBtn.hidden = true;
  finishBtn.hidden = true;

  if (state === 'recording') {
    //Show pause and finish controls while recording
    pauseResumeBtn.hidden = false;
    finishBtn.hidden = false;
    pauseResumeBtn.textContent = '⏸ Pause';
    statusEl.textContent = 'Recording... speak now.';

  } else if (state === 'paused') {
    //Show resume and finish controls while recording is paused
    pauseResumeBtn.hidden = false;
    finishBtn.hidden = false;
    pauseResumeBtn.textContent = '▶ Resume';
    statusEl.textContent = 'Recording paused.';

  } else if (state === 'processing') {
    //Inform the user that the audio is being transcribed
    statusEl.textContent = 'Processing transcription...';

  } else {
    //Show the start button when the application is ready
    recordBtn.hidden = false;
    recordBtn.textContent = 'Start Recording';
  }
}


//Starts the recording timer and resets the elapsed time
function startTimer() {
  elapsedBeforePauseMs = 0;
  recordingStartTime = Date.now();
  timerEl.textContent = '00:00';

  //Update the displayed time every 200 milliseconds
  timerIntervalId = setInterval(updateTimerDisplay, 200);
}


//Saves the time recorded so far and pauses the timer
function pauseTimer() {
  elapsedBeforePauseMs += Date.now() - recordingStartTime;
  clearInterval(timerIntervalId);
}


//Starts the timer again after a recording has been resumed
function resumeTimer() {
  recordingStartTime = Date.now();
  timerIntervalId = setInterval(updateTimerDisplay, 200);
}


//Stops the timer from updating
function stopTimer() {
  clearInterval(timerIntervalId);
}


//Calculates and displays the total active recording time
function updateTimerDisplay() {
  // Add the current recording period to any time recorded before a pause.
  const elapsedMs =
    elapsedBeforePauseMs + (Date.now() - recordingStartTime);

  //Convert milliseconds into minutes and seconds.
  const totalSeconds = Math.floor(elapsedMs / 1000);
  const minutes = String(Math.floor(totalSeconds / 60)).padStart(2, '0');
  const seconds = String(totalSeconds % 60).padStart(2, '0');

  //Display the elapsed recording time
  timerEl.textContent = `${minutes}:${seconds}`;
}
