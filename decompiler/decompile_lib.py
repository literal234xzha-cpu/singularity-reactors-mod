#!/usr/bin/env python3
"""
Native Library Decompiler for ELF .so files
Supports: ARM64, ARM, x86, x86_64
Usage: python3 decompile_lib.py <path_to_lib.so> [--mode asm|pseudo|find-patch]
"""

import sys
import struct
import os
import argparse
from capstone import *

# ELF constants
ELFCLASS32 = 1
ELFCLASS64 = 2
EM_ARM = 40
EM_ARM64 = 183
EM_X86_64 = 62
EM_386 = 3

# Architecture mapping
ARCH_MAP = {
    (ELFCLASS64, EM_ARM64): ("ARM64", CS_ARCH_ARM64, 0),
    (ELFCLASS32, EM_ARM): ("ARM", CS_ARCH_ARM, CS_MODE_32),
    (ELFCLASS32, EM_386): ("x86", CS_ARCH_X86, CS_MODE_32),
    (ELFCLASS64, EM_X86_64): ("x86_64", CS_ARCH_X86, CS_MODE_64),
}

SECTION_NAMES = {}

def read_elf_header(f, is_64):
    f.seek(0)
    magic = f.read(16)
    if magic[:4] != b'\x7fELF':
        raise ValueError("Not an ELF file")
    ei_class = magic[4]
    ei_data = magic[5]
    if ei_class == ELFCLASS64:
        f.seek(40)
        shoff = struct.unpack("<Q", f.read(8))[0]
        f.seek(58)
        shentsize = struct.unpack("<H", f.read(2))[0]
        shnum = struct.unpack("<H", f.read(2))[0]
        shstrndx = struct.unpack("<H", f.read(2))[0]
        return ei_class, ei_data, shoff, shentsize, shnum, shstrndx
    else:
        f.seek(32)
        shoff = struct.unpack("<I", f.read(4))[0]
        shentsize = struct.unpack("<H", f.read(2))[0]
        shnum = struct.unpack("<H", f.read(2))[0]
        shstrndx = struct.unpack("<H", f.read(2))[0]
        return ei_class, ei_data, shoff, shentsize, shnum, shstrndx

def read_section_headers(f, is_64, shoff, shentsize, shnum, shstrndx):
    sections = []
    f.seek(shoff)
    for i in range(shnum):
        data = f.read(shentsize)
        if is_64:
            sh_name, sh_type, sh_flags, sh_addr, sh_offset, sh_size, sh_link, sh_info, sh_addralign, sh_entsize = struct.unpack("<IIQQQQIIQQ", data)
        else:
            sh_name, sh_type, sh_flags, sh_addr, sh_offset, sh_size, sh_link, sh_info, sh_addralign, sh_entsize = struct.unpack("<iIIIIIIII", data)
        sections.append({
            'sh_name': sh_name, 'sh_type': sh_type, 'sh_flags': sh_flags,
            'sh_addr': sh_addr, 'sh_offset': sh_offset, 'sh_size': sh_size,
            'sh_link': sh_link, 'sh_info': sh_info, 'sh_addralign': sh_addralign, 'sh_entsize': sh_entsize
        })
    return sections

def get_section_names(f, sections, shstrndx):
    if shstrndx >= len(sections):
        return {}
    shstr_section = sections[shstrndx]
    f.seek(shstr_section['sh_offset'])
    strtab = f.read(shstr_section['sh_size'])
    names = {}
    for i, sec in enumerate(sections):
        offset = sec['sh_name']
        end = strtab.index(b'\x00', offset)
        names[i] = strtab[offset:end].decode('utf-8', errors='replace')
    return names

def find_code_sections(f, filepath):
    """Find executable sections in the ELF file"""
    ei_class, ei_data, shoff, shentsize, shnum, shstrndx = read_elf_header(f, None)
    is_64 = ei_class == ELFCLASS64
    sections = read_section_headers(f, is_64, shoff, shentsize, shnum, shstrndx)
    section_names = get_section_names(f, sections, shstrndx)
    
    code_sections = []
    for i, sec in enumerate(sections):
        name = section_names.get(i, "")
        flags = sec['sh_flags']
        # SHT_PROGBITS = 1, SHF_EXECINSTR = 0x4, SHF_ALLOC = 0x2
        if sec['sh_type'] == 1 and (flags & 0x4) and sec['sh_size'] > 0:
            code_sections.append({
                'name': name,
                'offset': sec['sh_offset'],
                'size': sec['sh_size'],
                'vaddr': sec['sh_addr'],
                'flags': flags
            })
    return code_sections, ei_class

def detect_arch(ei_class, ei_data, filepath):
    """Detect architecture from ELF header and machine field"""
    with open(filepath, 'rb') as f:
        if ei_data == 1:  # EI_DATA_LITTLE_ENDIAN
            endian = "<"
        else:
            endian = ">"
        f.seek(18)
        e_machine = struct.unpack(endian + "H", f.read(2))[0]
        arch_name, cs_arch, cs_mode = ARCH_MAP.get((ei_class, e_machine), (None, None, None))
        return arch_name, cs_arch, cs_mode, ei_class

def disassemble(filepath, start_offset, size, md, base_addr=0):
    with open(filepath, 'rb') as f:
        f.seek(start_offset)
        code = f.read(size)
        instructions = []
        for insn in md.disasm(code, base_addr):
            instructions.append({
                'address': hex(insn.address),
                'mnemonic': insn.mnemonic,
                'op_str': insn.op_str,
                'bytes': insn.bytes.hex()
            })
        return instructions

def decompile(filepath, mode="asm"):
    with open(filepath, 'rb') as f:
        ei_class, ei_data, shoff, shentsize, shnum, shstrndx = read_elf_header(f, None)
        arch_name, cs_arch, cs_mode, _ = detect_arch(ei_class, ei_data, filepath)
        if not arch_name:
            print(f"Error: Unsupported architecture (ELF class {ei_class}, machine {e_machine})")
            return None
        print(f"[*] Architecture: {arch_name} (cs_arch={cs_arch}, cs_mode={cs_mode})")
        
        f.seek(0)
        code_sections, _ = find_code_sections(f, filepath)
        
        if not code_sections:
            print("[!] No executable sections found")
            return None
        
        print(f"[*] Found {len(code_sections)} code sections:")
        for sec in code_sections:
            print(f"    - {sec['name']} @ offset 0x{sec['offset']:x} size 0x{sec['size']:x} vaddr 0x{sec['vaddr']:x}")
        
        md = Cs(cs_arch, cs_mode)
        if arch_name in ("ARM64", "x86_64"):
            md.detail = False
        
        all_instructions = []
        for sec in code_sections:
            print(f"\n[*] Disassembling {sec['name']}...")
            instrs = disassemble(filepath, sec['offset'], sec['size'], md, sec['vaddr'])
            all_instructions.extend(instrs)
        
        print(f"[*] Total instructions: {len(all_instructions)}")
        return {
            'arch': arch_name,
            'sections': code_sections,
            'instructions': all_instructions
        }

def find_potential_patches(filepath):
    """Look for potential SUB->ADD patches in native code"""
    with open(filepath, 'rb') as f:
        ei_class, ei_data, shoff, shentsize, shnum, shstrndx = read_elf_header(f, None)
        arch_name, cs_arch, cs_mode, _ = detect_arch(ei_class, ei_data, filepath)
        if not arch_name:
            print(f"Error: Unsupported architecture")
            return
        
        f.seek(0)
        code_sections, _ = find_code_sections(f, filepath)
        md = Cs(cs_arch, cs_mode)
        md.detail = True
        
        suspicious = []
        for sec in code_sections:
            f.seek(sec['offset'])
            code = f.read(sec['size'])
            for insn in md.disasm(code, sec['vaddr']):
                if arch_name == "ARM64":
                    if insn.mnemonic == "add":
                        for op in insn.operands:
                            if op.type == 0x1000:  # ARM64_OP_IMM
                                if 0x3E8 <= op.imm <= 0x10000:
                                    suspicious.append((insn.address, insn.mnemonic, insn.op_str))
                    if insn.mnemonic == "sub":
                        for op in insn.operands:
                            if op.type == 0x1000:
                                if 0x3E8 <= op.imm <= 0x10000:
                                    suspicious.append((insn.address, insn.mnemonic, insn.op_str))
                elif arch_name in ("x86", "x86_64"):
                    if insn.mnemonic in ("add", "sub"):
                        for op in insn.operands:
                            if op.type == 0x2:  # X86_OP_IMM
                                if 0x3E8 <= op.imm <= 0x10000:
                                    suspicious.append((insn.address, insn.mnemonic, insn.op_str))
                elif arch_name == "ARM":
                    if insn.mnemonic in ("add", "sub"):
                        for op in insn.operands:
                            if op.type == 0x2:  # ARM_OP_IMM
                                if 0x3E8 <= op.imm <= 0x10000:
                                    suspicious.append((insn.address, insn.mnemonic, insn.op_str))
        
        print(f"[*] Found {len(suspicious)} suspicious instructions (ADD/SUB with immediates 1000-65535):")
        for addr, mnem, ops in suspicious[:20]:
            print(f"    0x{addr:x}: {mnem} {ops}")
        if len(suspicious) > 20:
            print(f"    ... and {len(suspicious)-20} more")

def main():
    parser = argparse.ArgumentParser(description="ELF .so decompiler supporting ARM64, ARM, x86, x86_64")
    parser.add_argument("file", help="Path to the .so file")
    parser.add_argument("--mode", choices=["asm", "find-patch"], default="asm",
                       help="asm: full disassembly, find-patch: find potential SUB->ADD patches")
    parser.add_argument("-o", "--output", help="Output file for disassembly")
    args = parser.parse_args()
    
    if not os.path.exists(args.file):
        print(f"Error: File not found: {args.file}")
        sys.exit(1)
    
    print(f"[*] Analyzing: {args.file}")
    print(f"[*] File size: {os.path.getsize(args.file)} bytes")
    
    if args.mode == "find-patch":
        find_potential_patches(args.file)
    else:
        result = decompile(args.file, mode="asm")
        if result and args.output:
            with open(args.output, 'w') as out:
                out.write(f"# Decompilation report for {args.file}\n")
                out.write(f"# Architecture: {result['arch']}\n")
                out.write(f"# Total instructions: {len(result['instructions'])}\n\n")
                current_section = None
                for insn in result['instructions']:
                    if insn.get('section') and insn['section'] != current_section:
                        current_section = insn['section']
                        out.write(f"\n# Section: {current_section}\n")
                    out.write(f"{insn['address']}:\t{insn['mnemonic']}\t{insn['op_str']}  # {insn['bytes']}\n")
            print(f"[*] Output saved to: {args.output}")

if __name__ == "__main__":
    main()
