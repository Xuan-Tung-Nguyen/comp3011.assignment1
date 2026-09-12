//Get references to the UI elements.
const recordBtn = document.getElementById('recordBtn');
const statusEl = document.getElementById('status');
const transcriptEl = document.getElementById('transcript');

let mediaRecorder;
let audioChunks = [];
let isRecording = false;

