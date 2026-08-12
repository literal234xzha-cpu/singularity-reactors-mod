/*
 * decompile_lib.c - Native ELF .so decompiler
 * Supports: ARM64, ARM, x86, x86_64
 * Modes: --disasm (full), --patch (find SUB->ADD), --info (header info)
 * Usage: ./dl <file.so> [mode]
 * Filename smart-resolves: "libunity" -> libunity.so in cwd or subdirs
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <dirent.h>
#include <libgen.h>
#include <errno.h>

#define MAX_FILEPATH 4096
#define MAX_SECTIONS 128
#define MAX_INSTRUCTIONS 50000000

/* ELF constants */
#define EI_CLASS    4
#define EI_DATA     5
#define ELFCLASS32  1
#define ELFCLASS64  2
#define E_MACHINE   18

/* Section header types */
#define SHT_NULL    0
#define SHT_PROGBITS 1
#define SHT_SYMTAB  2
#define SHT_STRTAB  3
#define SHT_HASH    5
#define SHT_STRTAB  3

/* Section flags */
#define SHF_EXECINSTR 0x4
#define SHF_ALLOC     0x2

/* Symbol table constants */
#define STB_LOCAL  0
#define STT_FUNC   2

/* ELF32 structures */
typedef struct {
    uint32_t sh_name;
    uint32_t sh_type;
    uint32_t sh_flags;
    uint32_t sh_addr;
    uint32_t sh_offset;
    uint32_t sh_size;
    uint32_t sh_link;
    uint32_t sh_info;
    uint32_t sh_addralign;
    uint32_t sh_entsize;
} Elf32_Shdr;

/* ELF64 structures */
typedef struct {
    uint32_t sh_name;
    uint32_t sh_type;
    uint64_t sh_flags;
    uint64_t sh_addr;
    uint64_t sh_offset;
    uint64_t sh_size;
    uint32_t sh_link;
    uint32_t sh_info;
    uint64_t sh_addralign;
    uint64_t sh_entsize;
} Elf64_Shdr;

/* ELF64 symbol */
typedef struct {
    uint32_t st_name;
    uint8_t  st_info;
    uint8_t  st_other;
    uint16_t st_shndx;
    uint64_t st_value;
    uint64_t st_size;
} Elf64_Sym;

/* ELF32 symbol */
typedef struct {
    uint32_t st_name;
    uint32_t st_info;
    uint32_t st_shndx;
    uint64_t st_value;
    uint64_t st_size;
} Elf32_Sym;

#define ARM64_IS_ADDSUB_IMM_MASK  0x1F000000
#define ARM64_IS_ADDSUB_IMM_VAL   0x11000000

/* Instruction info */
typedef struct {
    uint64_t addr;
    uint32_t raw;
    char mnemonic[16];
    char operand[128];
    uint64_t immediate;
    int is_add_sub;
} InstInfo;

typedef struct {
    int elf_class;
    int ei_data;
    uint16_t e_machine;
    uint64_t shoff;
    uint64_t shnum;
    uint64_t shentsize;
    uint16_t shstrndx;
} ElfInfo;

typedef struct {
    char name[256];
    uint64_t offset;
    uint64_t size;
    uint64_t vaddr;
    uint64_t flags;
    uint32_t type;
    int index;
} CodeSection;

static const char* arch_names[] = {
    [0] = "UNKNOWN", [3] = "x86", [8] = "mips", [20] = "ppc",
    [21] = "ppc64", [24] = "sparc", [40] = "ARM", [183] = "ARM64",
    [62] = "x86_64", [243] = "riscv"
};

static const char* get_arch_name(uint16_t machine) {
    if (machine < sizeof(arch_names)/sizeof(arch_names[0]) && arch_names[machine])
        return arch_names[machine];
    return "UNKNOWN";
}

/* Smart filename resolution */
static int resolve_filename(const char* input, char* output) {
    /* If input contains / or ends with .so, use as-is */
    if (strchr(input, '/') || strstr(input, ".so")) {
        strncpy(output, input, MAX_FILEPATH-1);
        output[MAX_FILEPATH-1] = '\0';
        return 0;
    }

    char search_patterns[64][256];
    int n = 0;

    snprintf(search_patterns[n++], 256, "%s.so", input);
    snprintf(search_patterns[n++], 256, "lib%s.so", input);
    snprintf(search_patterns[n++], 256, "lib%s.so", strstr(input, "lib") == input ? input + 3 : input);

    const char* dirs[] = {".", "lib/", "lib/arm64-v8a/", "lib/armeabi-v7a/", "lib/x86/", "lib/x86_64/", NULL};

    for (int d = 0; dirs[d]; d++) {
        DIR* dir = opendir(dirs[d]);
        if (!dir) continue;
        struct dirent* ent;
        while ((ent = readdir(dir)) != NULL) {
            for (int p = 0; p < n; p++) {
                if (strstr(ent->d_name, search_patterns[p]) ||
                    (strcmp(ent->d_name, search_patterns[p]) == 0)) {
                    snprintf(output, MAX_FILEPATH, "%s/%s", dirs[d], ent->d_name);
                    closedir(dir);
                    return 0;
                }
            }
        }
        closedir(dir);
    }

    /* Fallback: search recursively */
    const char* base_dirs[] = {".", "decompiler/output/epic_stickman_apktool/", NULL};
    for (int b = 0; base_dirs[b]; b++) {
        char cmd[1024];
        snprintf(cmd, sizeof(cmd), "find %s -name '%s.so' -o -name 'lib%s.so' 2>/dev/null | head -1",
                 base_dirs[b], input, input);
        FILE* fp = popen(cmd, "r");
        if (fp) {
            if (fgets(output, MAX_FILEPATH, fp) != NULL) {
                output[strcspn(output, "\n")] = '\0';
                pclose(fp);
                if (strlen(output) > 0) return 0;
            }
            pclose(fp);
        }
    }

    return -1;
}

static int read_elf_header(int fd, ElfInfo* info) {
    uint8_t ident[16];
    if (lseek(fd, 0, SEEK_SET) == (off_t)-1) return -1;
    if (read(fd, ident, 16) != 16) return -1;
    static const char elf_magic[] = {0x7f, 'E', 'L', 'F'};
    if (memcmp(ident, elf_magic, 4) != 0) return -1;

    info->elf_class = ident[EI_CLASS];
    info->ei_data = ident[EI_DATA];

    if (info->elf_class == ELFCLASS64) {
        uint64_t shoff; uint16_t shentsize, shnum, shstrndx;
        lseek(fd, 40, SEEK_SET);
        read(fd, &shoff, 8);
        lseek(fd, 58, SEEK_SET);
        read(fd, &shentsize, 2);
        read(fd, &shnum, 2);
        read(fd, &shstrndx, 2);
        info->shoff = shoff; info->shentsize = shentsize;
        info->shnum = shnum; info->shstrndx = shstrndx;
        lseek(fd, 18, SEEK_SET);
        read(fd, &info->e_machine, 2);
    } else {
        uint32_t shoff; uint16_t shentsize, shnum, shstrndx;
        lseek(fd, 32, SEEK_SET);
        read(fd, &shoff, 4);
        lseek(fd, 46, SEEK_SET);
        read(fd, &shentsize, 2);
        read(fd, &shnum, 2);
        read(fd, &shstrndx, 2);
        info->shoff = shoff; info->shentsize = shentsize;
        info->shnum = shnum; info->shstrndx = shstrndx;
        lseek(fd, 18, SEEK_SET);
        read(fd, &info->e_machine, 2);
    }
    return 0;
}

static int get_section_names(int fd, ElfInfo* info, char* strtab, size_t strtab_size, char names[][256]) {
    if (info->shstrndx >= info->shnum) return -1;
    if (info->elf_class == ELFCLASS64) {
        Elf64_Shdr sh;
        lseek(fd, info->shoff + info->shstrndx * sizeof(Elf64_Shdr), SEEK_SET);
        read(fd, &sh, sizeof(Elf64_Shdr));
        lseek(fd, sh.sh_offset, SEEK_SET);
        read(fd, strtab, sh.sh_size);
        strtab_size = sh.sh_size;
    } else {
        Elf32_Shdr sh;
        lseek(fd, info->shoff + info->shstrndx * sizeof(Elf32_Shdr), SEEK_SET);
        read(fd, &sh, sizeof(Elf32_Shdr));
        lseek(fd, sh.sh_offset, SEEK_SET);
        read(fd, strtab, sh.sh_size);
        strtab_size = sh.sh_size;
    }
    /* Parse section names */
    for (int i = 0; i < info->shnum; i++) {
        if (info->elf_class == ELFCLASS64) {
            Elf64_Shdr sh;
            lseek(fd, info->shoff + i * sizeof(Elf64_Shdr), SEEK_SET);
            read(fd, &sh, sizeof(Elf64_Shdr));
            if (sh.sh_name < strtab_size) {
                strncpy(names[i], strtab + sh.sh_name, 255);
            }
        } else {
            Elf32_Shdr sh;
            lseek(fd, info->shoff + i * sizeof(Elf32_Shdr), SEEK_SET);
            read(fd, &sh, sizeof(Elf32_Shdr));
            if (sh.sh_name < strtab_size) {
                strncpy(names[i], strtab + sh.sh_name, 255);
            }
        }
    }
    return 0;
}

static void decode_arm64_add_sub(uint32_t insn, uint64_t* imm, int* is_add) {
    *is_add = 0;
    *imm = 0;
    if ((insn & ARM64_IS_ADDSUB_IMM_MASK) == ARM64_IS_ADDSUB_IMM_VAL) {
        int op = (insn >> 30) & 1;
        uint64_t imm12 = (insn >> 10) & 0xFFF;
        uint64_t shift = (insn >> 22) & 0x3;
        *imm = imm12 << (12 * shift);
        *is_add = (op == 0) ? 1 : 0;
    }
}

static void analyze_code_section(const char* filepath, CodeSection* sec, int is_64, int machine) {
    int fd = open(filepath, O_RDONLY);
    if (fd < 0) return;

    void* mapped = mmap(NULL, sec->size, PROT_READ, MAP_PRIVATE, fd, sec->offset);
    if (mapped == MAP_FAILED) { close(fd); return; }

    if (is_64 && machine == 183) {
        /* ARM64: scan for ADD/SUB immediate patterns */
        const uint32_t* code = (const uint32_t*)mapped;
        size_t count = sec->size / 4;
        int suspicious_add = 0, suspicious_sub = 0;

        for (size_t i = 0; i < count; i++) {
            uint32_t insn = code[i];
            int is_add; uint64_t imm;
            decode_arm64_add_sub(insn, &imm, &is_add);

            if (imm >= 0x3E8 && imm <= 0x10000) {
                if (is_add) {
                    suspicious_add++;
                    if (suspicious_add <= 50)
                        printf("  0x%llx: ADD #%llu (potential SUB->ADD patch)\n",
                               (unsigned long long)(sec->vaddr + i*4), (unsigned long long)imm);
                } else {
                    suspicious_sub++;
                }
            }
        }

        printf("  Found %d ADD and %d SUB with immediates 1000-65535\n", suspicious_add, suspicious_sub);
        if (suspicious_add > 0)
            printf("  *** %d potential SUB->ADD patches detected! ***\n", suspicious_add);
        else
            printf("  No suspicious patches found\n");

    } else {
        /* Use objdump for other architectures */
        char cmd[1024];
        const char* arch_flag = "";
        switch (machine) {
            case 62: arch_flag = "i386"; break;
            case 3:  arch_flag = "i386"; break;
            case 40: arch_flag = "arm"; break;
            default: arch_flag = "auto"; break;
        }
        snprintf(cmd, sizeof(cmd),
                 "objdump -D -b binary -m %s --adjust-vma=0x%llx %s 2>/dev/null | "
                 "grep -E 'add|sub' | "
                 "awk '$2 ~ /#[0-9]+/ { gsub(\"#\",\"\"); v=strtonum($2); if(v>=1000 && v<=65535) print \"  \"$0 }'",
                 arch_flag, (unsigned long long)sec->offset, filepath);

        FILE* fp = popen(cmd, "r");
        if (fp) {
            char line[512];
            int count = 0;
            while (fgets(line, sizeof(line), fp) != NULL) {
                if (count < 50) printf("%s", line);
                count++;
            }
            if (count > 50) printf("  ... %d more lines\n", count - 50);
            pclose(fp);
        }
    }

    munmap(mapped, sec->size);
    close(fd);
}

int main(int argc, char* argv[]) {
    if (argc < 2) {
        printf("Usage: %s <file.so> [--disasm|--patch|--info]\n", argv[0]);
        printf("  Modes:\n");
        printf("    (default) --info    : Show ELF header info\n");
        printf("    --patch             : Find SUB->ADD patches (gold hack detection)\n");
        printf("    --disasm            : Full disassembly via objdump\n");
        printf("  File can be full path, filename, or abbreviated name.\n");
        printf("  Examples:\n");
        printf("    dl libunity.so\n");
        printf("    dl il2cpp --patch\n");
        printf("    dl libil2cpp.so --disasm\n");
        return 1;
    }

    char resolved_path[MAX_FILEPATH];
    int result = resolve_filename(argv[1], resolved_path);
    if (result != 0) {
        fprintf(stderr, "Error: Could not find file '%s'\n", argv[1]);
        fprintf(stderr, "Search locations: current dir, lib/, lib/arm64-v8a/, lib/armeabi-v7a/\n");
        return 1;
    }

    const char* mode = "--info";
    if (argc >= 3) {
        for (int i = 2; i < argc; i++) {
            if (strcmp(argv[i], "--patch") == 0 || strcmp(argv[i], "--disasm") == 0 || strcmp(argv[i], "--info") == 0) {
                mode = argv[i];
            }
        }
    }

    int fd = open(resolved_path, O_RDONLY);
    if (fd < 0) {
        fprintf(stderr, "Error: Cannot open %s: %s\n", resolved_path, strerror(errno));
        return 1;
    }

    ElfInfo info;
    if (read_elf_header(fd, &info) != 0) {
        fprintf(stderr, "Error: Not a valid ELF file\n");
        close(fd);
        return 1;
    }

    const char* arch = get_arch_name(info.e_machine);
    printf("[*] File: %s\n", resolved_path);
    printf("[*] ELF class: %d-bit, endianness: %s\n", info.elf_class * 32, info.ei_data == 1 ? "LE" : "BE");
    printf("[*] Architecture: %s (machine=0x%x)\n", arch, info.e_machine);

    /* Get section headers */
    char names[MAX_SECTIONS][256];
    memset(names, 0, sizeof(names));
    char strtab[1024];
    get_section_names(fd, &info, strtab, sizeof(strtab), names);

    CodeSection code_sections[MAX_SECTIONS];
    int num_code = 0;

    for (int i = 0; i < info.shnum && num_code < MAX_SECTIONS; i++) {
        if (info.elf_class == ELFCLASS64) {
            Elf64_Shdr sh;
            lseek(fd, info.shoff + i * sizeof(Elf64_Shdr), SEEK_SET);
            read(fd, &sh, sizeof(Elf64_Shdr));
            /* SHT_PROGBITS = 1, SHF_EXECINSTR = 0x4, SHF_ALLOC = 0x2 */
            if (sh.sh_type == 1 && (sh.sh_flags & 0x4) && sh.sh_size > 0) {
                code_sections[num_code].offset = sh.sh_offset;
                code_sections[num_code].size = sh.sh_size;
                code_sections[num_code].vaddr = sh.sh_addr;
                code_sections[num_code].flags = sh.sh_flags;
                code_sections[num_code].type = sh.sh_type;
                code_sections[num_code].index = i;
                strncpy(code_sections[num_code].name, names[i], 255);
                num_code++;
            }
        } else {
            Elf32_Shdr sh;
            lseek(fd, info.shoff + i * sizeof(Elf32_Shdr), SEEK_SET);
            read(fd, &sh, sizeof(Elf32_Shdr));
            if (sh.sh_type == 1 && (sh.sh_flags & 0x4) && sh.sh_size > 0) {
                code_sections[num_code].offset = sh.sh_offset;
                code_sections[num_code].size = sh.sh_size;
                code_sections[num_code].vaddr = sh.sh_addr;
                code_sections[num_code].flags = sh.sh_flags;
                code_sections[num_code].type = sh.sh_type;
                code_sections[num_code].index = i;
                strncpy(code_sections[num_code].name, names[i], 255);
                num_code++;
            }
        }
    }

    printf("[*] Found %d code sections:\n", num_code);
    for (int i = 0; i < num_code; i++) {
        printf("  - %s @ offset=0x%llx size=0x%llx vaddr=0x%llx\n",
               code_sections[i].name,
               (unsigned long long)code_sections[i].offset,
               (unsigned long long)code_sections[i].size,
               (unsigned long long)code_sections[i].vaddr);
    }

    if (strcmp(mode, "--info") == 0 || strcmp(mode, "info") == 0) {
        /* Just print info, already done above */
    } else if (strcmp(mode, "--patch") == 0 || strcmp(mode, "patch") == 0) {
        printf("\n[*] Scanning for SUB->ADD patches...\n");
        for (int i = 0; i < num_code; i++) {
            printf("\n[%s]\n", code_sections[i].name);
            analyze_code_section(resolved_path, &code_sections[i],
                               info.elf_class == ELFCLASS64, info.e_machine);
        }
    } else if (strcmp(mode, "--disasm") == 0 || strcmp(mode, "disasm") == 0) {
        printf("\n[*] Full disassembly (using objdump)...\n");
        char cmd[2048];
        const char* arch_flag = "auto";
        switch (info.e_machine) {
            case 183: arch_flag = "aarch64"; break;
            case 62:  arch_flag = "i386:x86-64"; break;
            case 40:  arch_flag = "arm"; break;
            case 3:   arch_flag = "i386"; break;
            case 243: arch_flag = "riscv"; break;
        }
        snprintf(cmd, sizeof(cmd),
                 "objdump -D -b binary -m %s %s 2>&1 | head -50000",
                 arch_flag, resolved_path);
        printf("[*] Running: %s\n", cmd);
        system(cmd);
    }

    close(fd);
    return 0;
}
