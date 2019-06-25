# ADB Setup Guide

# Device Setup
 - Go to Settings >> Developer Options and enable USB Debugging.
     - Enable Developer Options by going to Settings >> About, finding the Build Number item and tapping it 7 times.
 - Plug your device into your computer and accept the authorization request on it.
     - (Windows) You may need to install your device's drivers, available from the manufacturer's website.

# Windows Setup
 - Download the ADB ZIP for Windows: https://dl.google.com/android/repository/platform-tools-latest-windows.zip.
 - Extract the ZIP to an easily-accessible location (ie Desktop).
 - Open CMD.
 - Enter `cd C:\path\to\extracted\folder\` to get to the folder you extracted ADB into. For instance, `C:\Users\Zachary\Desktop\platform-tools\`.
 
# Linux (Debian-based) Setup
 - Make sure `adb` is installed:
     - `sudo apt-get install adb`

# Linux (Fedora-based/openSUSE-based) Setup
 - Make sure `android-tools` is installed:
     - `sudo yum install android-tools`

# Linux (Other) Setup
 - Download the ADB ZIP for Linux: https://dl.google.com/android/repository/platform-tools-latest-linux.zip
 - Extract the ZIP to an easily-accessible location (ie Desktop).
 - Open Terminal.
 - Enter `cd /path/to/extracted/folder/` to get to the folder you extracted ADB into. For instance, `/home/Zachary/Desktop/platform-tools/`.

# macOS Setup
 - Download the ADB ZIP for macOS: https://dl.google.com/android/repository/platform-tools-latest-darwin.zip
 - Extract the ZIP to an easily-accessible location (ie Desktop).
 - Open Terminal.
 - Enter `cd /path/to/extracted/folder/` to get to the folder you extracted ADB into. For instance, `/Users/Zachary/Desktop/platform-tools/`.
 
# Running the Commands
 - (NOTE) On Linux (Other) and macOS, use `./adb` instead of `adb`.
 - In CMD/Terminal, run `adb devices` to make sure your device is seen.
 - Run these commands:
     - `adb shell pm grant com.zacharee1.systemuituner android.permission.WRITE_SECURE_SETTINGS`
     - `adb shell pm grant com.zacharee1.systemuituner android.permission.DUMP`
 - If neither of those commands has any output, then everything was successful and you can use the app.
