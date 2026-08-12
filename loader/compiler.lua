-- loader/compiler.lua
-- Convierte source Lua (simplificado) a bytecode custom cifrado.
-- MVP: trata todo el source como un string literal a imprimir (opcode STR + PUT + END).
local M = {}

local XOR_KEY = 0x5A3C
local SEED = 1729183

-- bytecode builder
local function b8(n)
  return string.char(n % 256, math.floor(n / 256) % 256)
end

function M.compile(src)
  local payload = src
  local len = #payload
  -- opcode STR(1) + len(2 bytes LE) + payload + PUT(3) + END(4)
  local parts = {
    string.char(1),
    string.char(len % 256, math.floor(len / 256) % 256),
    payload,
    string.char(3),
    string.char(4),
  }
  return table.concat(parts)
end

-- cifrado XOR con keystream derivado
function M.encrypt(bytecode)
  local key = XOR_KEY
  local out = {}
  for i = 1, #bytecode do
    local b = string.byte(bytecode, i)
    local k = (key * (i + SEED)) % 256
    out[i] = string.char(b ~ k)
  end
  return table.concat(out)
end

-- API: source -> bytecode cifrado (string)
function M.obfuscate(src)
  local bc = M.compile(src)
  return M.encrypt(bc)
end

return M
