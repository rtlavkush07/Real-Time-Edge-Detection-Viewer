🖼️ Real-Time Edge Detection Viewer


✨ Features Implemented
📱 Android

-Camera feed integration using TextureView

-Frame processing via OpenCV (C++) using JNI

-Grayscale filter

-Canny edge detection

-Render processed frames using OpenGL ES 2.0

-Real-time performance (≥10–15 FPS)

🌐 Web

-Minimal TypeScript + HTML page

-Displays a static processed frame (captured from Android app)

-Basic overlay for frame stats (FPS, resolution)

📸 Screenshots / GIFs

![App Screenshot](https://github.com/rtlavkush07/Real-Time-Edge-Detection-Viewer/blob/main/Output%20Screenshots/output2.jpg?raw=true)


![App Screenshot](https://github.com/rtlavkush07/Real-Time-Edge-Detection-Viewer/blob/main/Output%20Screenshots/output3.jpg?raw=true)


![App Screenshot](https://github.com/rtlavkush07/Real-Time-Edge-Detection-Viewer/blob/main/Output%20Screenshots/output4.jpg?raw=true)


![App Output GIF](https://github.com/rtlavkush07/Real-Time-Edge-Detection-Viewer/blob/main/Output%20Screenshots/ouptput%20phone%20gif.gif?raw=true)


⚙️ Setup Instructions
📱 Android

-Clone the repository:

-git clone <repo_url>


-Install Android Studio (latest version recommended)

-Install NDK and CMake from SDK Manager

-Place OpenCV SDK in the project folder

-Configure CMakeLists.txt to include OpenCV headers and link libraries

-Connect an Android device (minSdk 24) and run the app

🌐 Web Viewer

-Open index.html in a browser

-Ensure sample_frame.jpg is in the same folder

-The image will display FPS and resolution overlay

🏗️ Architecture Overview

Frame Flow:

-Camera (Android) → JNI → OpenCV C++ Processing → OpenGL ES Renderer → Display → Web Viewer


-Android (Java/Kotlin): Captures camera frames and sets up UI/OpenGL surface

-JNI: Passes frames between Android and C++ native code

-C++ (OpenCV): Processes frames with Grayscale / Canny Edge Detection



OpenGL ES: Renders processed frames in real-time

Web Viewer (TypeScript + HTML): Displays static processed frame and basic frame stats
