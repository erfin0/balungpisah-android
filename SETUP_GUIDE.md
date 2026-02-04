# BalungPisah Android App - Complete Setup Guide

## Quick Start

1. **Configure API endpoints** in `AppConfig.kt`
2. **Open project** in Android Studio
3. **Sync Gradle**
4. **Run the app**

## Detailed Setup

### Step 1: Install Prerequisites

#### Required Software

- **Android Studio Hedgehog (2023.1.1) or later**
  - Download: https://developer.android.com/studio
  
- **JDK 17**
  - Usually bundled with Android Studio
  - Or download from: https://adoptium.net/

#### Recommended

- Android device or emulator running Android 7.0+ (API 24)

### Step 2: Configure the App

#### 2.1 API Configuration

Open `app/src/main/java/com/balungpisah/util/AppConfig.kt` and replace placeholders:

```kotlin
object AppConfig {
    val environment: AppEnvironment = AppEnvironment.PRODUCTION
    
    // Your backend API URL
    const val API_BASE_URL = "https://api.balungpisah.com"
    // or for development: "http://10.0.2.2:8080"
    
    // Your Logto authentication server
    const val LOGTO_ENDPOINT = "https://auth.balungpisah.com"
    
    const val API_VERSION = "v1"
    
    // Logto app credentials from your Logto console
    const val LOGTO_APP_ID = "abc123xyz"
    const val LOGTO_APP_SECRET = "secret_abc123"
    
    // This should match what's registered in Logto
    const val LOGTO_REDIRECT_URI = "com.balungpisah://callback"
    const val LOGTO_SCOPES = "openid profile email offline_access"
}
```

#### 2.2 Development vs Production

For development (testing with localhost):

```kotlin
val environment: AppEnvironment = AppEnvironment.DEVELOPMENT

// For Android Emulator
const val API_BASE_URL = "http://10.0.2.2:8080"

// For Physical Device (use your computer's IP)
const val API_BASE_URL = "http://192.168.1.100:8080"
```

To find your computer's IP:
- **Windows**: Run `ipconfig` in Command Prompt
- **Mac/Linux**: Run `ifconfig` or `ip addr` in Terminal

#### 2.3 Logto Setup

1. Go to your Logto Console
2. Create a new application (Native App)
3. Add redirect URI: `com.balungpisah://callback`
4. Copy the App ID and App Secret to `AppConfig.kt`

### Step 3: Open Project in Android Studio

1. Launch Android Studio
2. Click **"Open"**
3. Navigate to `BalungPisahApp` folder
4. Click **"OK"**
5. Wait for Gradle sync to complete (may take 5-10 minutes first time)

### Step 4: Resolve Dependencies

If Gradle sync shows errors:

1. **Check internet connection** - Dependencies download from Maven Central
2. **Update Gradle** - Android Studio may prompt you to update
3. **Invalidate caches**:
   - File → Invalidate Caches / Restart
   - Click "Invalidate and Restart"

### Step 5: Add App Logo (Optional)

Replace placeholder boxes with your actual logo:

1. Add logo image to `app/src/main/res/drawable/`
   - Example: `logo_balungpisah.png`

2. Update launcher icons in `app/src/main/res/mipmap-*/`
   - Use Android Studio's Image Asset tool:
     - Right-click `res` folder
     - New → Image Asset
     - Select your logo
     - Generate icons

3. Update code to use your logo:

In `ChatScreen.kt`, `LoginScreen`, `SettingsScreen`, etc., replace:

```kotlin
// Old placeholder
Box(
    modifier = Modifier
        .size(120.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primary)
)

// New with your logo
Image(
    painter = painterResource(id = R.drawable.logo_balungpisah),
    contentDescription = "BalungPisah Logo",
    modifier = Modifier.size(120.dp)
)
```

### Step 6: Run the App

#### On Emulator

1. Click **"Device Manager"** in Android Studio
2. Click **"Create Device"**
3. Select a device (e.g., Pixel 6)
4. Select system image (Android 14 / API 34 recommended)
5. Click **"Finish"**
6. Click **Run** (green play button) or press **Shift+F10**

#### On Physical Device

1. Enable Developer Options on your device:
   - Go to Settings → About Phone
   - Tap "Build Number" 7 times
   
2. Enable USB Debugging:
   - Go to Settings → Developer Options
   - Enable "USB Debugging"
   
3. Connect device via USB

4. In Android Studio:
   - Select your device from device dropdown
   - Click **Run**

### Step 7: Test the App

1. **Login Flow**:
   - Click "Masuk dengan Logto"
   - Browser should open with Logto login page
   - After login, app should return and show chat screen

2. **Chat Flow**:
   - Type a message
   - Click send
   - Should see streaming response from AI

3. **Navigation**:
   - Test bottom navigation tabs
   - Open history drawer (menu icon)
   - Open settings drawer (settings icon)

## Troubleshooting

### Issue: Gradle Sync Failed

**Solution**:
```bash
# In Terminal (inside project folder)
./gradlew clean
./gradlew build --refresh-dependencies
```

### Issue: Cannot connect to API

**Symptoms**: Network errors, timeouts

**Solutions**:
1. Check `API_BASE_URL` is correct
2. For localhost, use `10.0.2.2` (emulator) or your computer's IP (device)
3. Ensure backend is running
4. Check `AndroidManifest.xml` has:
   ```xml
   <uses-permission android:name="android.permission.INTERNET" />
   ```

### Issue: Logto login fails

**Symptoms**: "Unauthorized", "Invalid client"

**Solutions**:
1. Verify `LOGTO_APP_ID` matches Logto Console
2. Check redirect URI `com.balungpisah://callback` is registered in Logto
3. Ensure intent-filter in `AndroidManifest.xml`:
   ```xml
   <intent-filter>
       <action android:name="android.intent.action.VIEW" />
       <category android:name="android.intent.category.DEFAULT" />
       <category android:name="android.intent.category.BROWSABLE" />
       <data
           android:scheme="com.balungpisah"
           android:host="callback" />
   </intent-filter>
   ```

### Issue: SSE streaming not working

**Symptoms**: Messages not appearing, loading forever

**Solutions**:
1. Check backend SSE endpoint is accessible
2. Verify token is being sent in Authorization header
3. Check Logcat for errors:
   - View → Tool Windows → Logcat
   - Filter by "SSEClient" tag

### Issue: App size too large

**Solutions**:
1. Ensure ProGuard is enabled in `build.gradle.kts`:
   ```kotlin
   release {
       isMinifyEnabled = true
       isShrinkResources = true
   }
   ```
2. Remove unused resources
3. Use WebP images instead of PNG
4. Enable R8 full mode

## Building for Release

### Generate Signed APK

1. Build → Generate Signed Bundle / APK
2. Select APK
3. Create or select keystore
4. Fill in keystore details
5. Select "release" build variant
6. Click "Finish"

APK will be in: `app/release/app-release.apk`

### App Signing

For Google Play Store, you need to sign your app.

1. **Create a keystore** (one-time):
   ```bash
   keytool -genkey -v -keystore balungpisah.keystore \
     -alias balungpisah -keyalg RSA -keysize 2048 -validity 10000
   ```

2. **Keep keystore safe** - You cannot recover it if lost!

3. **Sign APK**:
   - In Android Studio: Build → Generate Signed Bundle
   - Or command line:
   ```bash
   jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
     -keystore balungpisah.keystore app-release-unsigned.apk balungpisah
   ```

## Performance Optimization

### Reduce APK Size

1. **Enable ProGuard** (already configured)
2. **Use WebP images**:
   - Right-click image in `res/drawable`
   - Convert to WebP
   
3. **Remove unused resources**:
   ```kotlin
   android {
       buildTypes {
           release {
               isShrinkResources = true
           }
       }
   }
   ```

4. **Split APKs by ABI**:
   ```kotlin
   android {
       splits {
           abi {
               enable = true
               reset()
               include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
               universalApk = false
           }
       }
   }
   ```

### Improve Performance

1. **Enable R8**:
   ```properties
   # gradle.properties
   android.enableR8.fullMode=true
   ```

2. **Use baseline profiles** (for Jetpack Compose)

3. **Lazy load images** with Coil (already integrated)

## Development Tips

### Debugging

1. **Enable verbose logging**:
   ```kotlin
   // In AppConfig.kt
   const val DEBUG_MODE = true
   ```

2. **View logs** in Logcat:
   - Filter by package: `com.balungpisah`
   - Or by tag: `SSEClient`, `NetworkClient`, etc.

### Hot Reload

Compose supports hot reload:
1. Make UI changes
2. Press **Ctrl+Shift+F10** (Windows/Linux) or **Cmd+Shift+R** (Mac)
3. Changes apply immediately without rebuilding

### Testing on Different Screen Sizes

Use Android Studio's Device Manager to test on:
- Phone (Pixel 6)
- Tablet (Pixel Tablet)
- Foldable (Pixel Fold)

## Next Steps

1. **Customize branding** - Add your logo and colors
2. **Implement Dashboard** - Add statistics and charts
3. **Implement Reports** - Show user's report history
4. **Add file upload** - Allow image attachments in chat
5. **Add notifications** - Push notifications for report updates
6. **Localization** - Add support for multiple languages

## Resources

- **Android Developers**: https://developer.android.com
- **Jetpack Compose**: https://developer.android.com/jetpack/compose
- **Logto Documentation**: https://docs.logto.io
- **OkHttp**: https://square.github.io/okhttp
- **Material Design 3**: https://m3.material.io

## Support

If you encounter issues:

1. Check this guide
2. Search GitHub issues
3. Check Logcat for error messages
4. Contact development team

---

**Good luck with your app development!** 🚀
