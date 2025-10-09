var frame = document.getElementById('frame');
var stats = document.getElementById('stats');
// Example data
var fps = 30;
var resolution = { width: 1280, height: 720 };
// Update stats dynamically
stats.textContent = "FPS: ".concat(fps, " | Resolution: ").concat(resolution.width, "x").concat(resolution.height);
// If you want to update frame later (simulating new processed frames):
// frame.src = 'new_frame.png';
