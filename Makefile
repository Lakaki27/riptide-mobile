.PHONY: clean-build-and-debug logcat

clean-build-and-debug:
	./gradlew clean build && ./gradlew installDebug

logcat:
	adb logcat --pid=$(NUMBER)

get-debug-number:
	adb shell pidof -s com.evertide.riptide