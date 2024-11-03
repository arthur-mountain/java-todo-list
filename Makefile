# 定義 JAR 檔案的名稱和路徑
JAR_FILE=app/build/libs/app-all.jar

all: build

# 使用 gradlew 構建 Fat JAR
build:
	@echo "\033[33mBuilding the project with Gradle...\033[0m"
	./gradlew fatJar
	@echo "\033[33mBuild completed.\033[0m\n"

# 檢查 JAR 檔案是否存在並運行
run: build
	@echo "\033[33mRunning the application...\033[0m"
	@if [ -f "$(JAR_FILE)" ]; then \
	    java -jar "$(JAR_FILE)"; \
	else \
	    echo "Error: JAR file not found at $(JAR_FILE)"; \
	    exit 1; \
	fi

test:
	@echo "\033[33mRunning test with Gradle...\033[0m"
	./gradlew test
	@echo "\033[33mRunning test completed.\033[0m\n"
