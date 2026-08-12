# Makefile for decompile_lib - Native ELF .so decompiler
# No Python dependencies required - pure C binary

CC = gcc
CFLAGS = -O2 -Wall -Wno-unused-result

# Output binary
TARGET = dl

# Source files
SRCS = decompiler/decompile_lib.c
OBJS = $(SRCS:.c=.o)

.PHONY: all clean install

all: $(TARGET)

$(TARGET): $(SRCS)
	$(CC) $(CFLAGS) -o $@ $<

clean:
	rm -f $(TARGET) $(OBJS)

install: $(TARGET)
	cp $(TARGET) /usr/local/bin/$(TARGET)

# One-liner to run
run: $(TARGET)
	./$(TARGET)
