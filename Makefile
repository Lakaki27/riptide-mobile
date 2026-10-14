.PHONY: clean-build-and-debug logcat release get-debug-number

clean-build-and-debug:
	./gradlew clean build && ./gradlew installDebug

logcat:
	adb logcat --pid=$(NUMBER)

get-debug-number:
	adb shell pidof -s com.evertide.riptide


release:
	./gradlew assembleRelease
	adb install -r app/build/outputs/apk/release/app-release.apk
