-- loader/loader.lua
-- Runtime VM + descifrado.  Recibe bytecode custom cifrado y lo ejecuta.
-- Anti-debug basico: aborta si detecta entorno de inspeccion.
local M = {}

local XOR_KEY = 0x5A3C
local SEED = 1729183

local function xor_decrypt(src, key)
  local out = {}
  for i = 1, #src do
    local b = string.byte(src, i)
    local k = (key * (i + SEED)) % 256
    out[i] = string.char(b ~ k)
  end
  return table.concat(out)
end

-- detecta si alguien quiere debuguear (lua -j dump, luajit -j dumper, debug hooks)
local function detect_debug()
  local fenv = getfenv and getfenv()
  if jit and jit.os then return false end
  -- lua5.1: _G.debug accesible?
  if rawget(_G, "debug") then return true end
  return false
end

-- mini VM: interpreta opcodes custom (STR=1, PUT=3, NOP=0, JMP=2, END=4)
local function run_vm(bytecode)
  if detect_debug() then
    error("entorno bloqueado")
  end

  -- Self-modify: permuta bytecode con SEED antes de ejecutar (anti-disassemly)
  local permuted = {}
  for i = 1, #bytecode do
    local p = ((i * 7 + SEED) % #bytecode) + 1
    permuted[i] = bytecode:sub(p, p)
  end
  local code = table.concat(permuted)

  local pc = 1
  local stk = {}
  while pc <= #code do
    local op = string.byte(code, pc)
    pc = pc + 1
    if op == 1 then  -- STR: len(2 bytes LE) + payload
      local len = string.byte(code, pc) + string.byte(code, pc + 1) * 256
      pc = pc + 2
      stk[#stk + 1] = code:sub(pc, pc + len - 1)
      pc = pc + len
    elseif op == 3 then  -- PUT
      print(stk[#stk] or "")
      stk[#stk] = nil
    elseif op == 2 then  -- JMP addr(4 bytes LE)
      local addr = 0
      for j = 0, 3 do
        addr = addr + string.byte(code, pc + j) * (256 ^ j)
      end
      pc = addr
    elseif op == 4 then  -- END
      break
    elseif op == 0 then  -- NOP
    else
      break  -- opcode desconocido: abortar
    end
  end
  return true
end

function M.execute(cipher_bytes)
  local plain = xor_decrypt(cipher_bytes, XOR_KEY)
  return run_vm(plain)
end

function M.decrypt_only(cipher_bytes)
  return xor_decrypt(cipher_bytes, XOR_KEY)
end

return M
