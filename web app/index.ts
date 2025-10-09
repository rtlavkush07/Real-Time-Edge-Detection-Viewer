const frame = document.getElementById('frame') as HTMLImageElement;
const stats = document.getElementById('stats') as HTMLDivElement;

// Example data
const fps = 30;
const resolution = { width: 1280, height: 720 };

// Update stats dynamically
stats.textContent = `FPS: ${fps} | Resolution: ${resolution.width}x${resolution.height}`;

