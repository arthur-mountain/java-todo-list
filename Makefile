PKG=$(word 2, $(MAKECMDGOALS))
SRC_DIR=app/src/main/java/$(PKG)
BIN_DIR=app/bin/main/java
SOURCES=$(SRC_DIR)/*.java # Directly pass the wildcard pattern to javac.
# SOURCES=$(wildcard $(SRC_DIR)/*.java) # Expand the wildcard pattern to a list of files as arguments for javac.
MAIN_CLASS=$(PKG).$(word 3, $(MAKECMDGOALS))

all: compile

compile:
	@echo "\033[33mCompiling Java files...\033[0m"
	javac -d $(BIN_DIR) $(SOURCES)
	@echo "\033[33mCompiled Java files...\n\033[0m"

run: compile
	@echo "\033[33mRunning the application...\033[0m"
	java -cp $(BIN_DIR) $(MAIN_CLASS)
