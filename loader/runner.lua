-- runner.lua: ofusca source -> bytecode custom cifrado -> ejecuta con loader.
local comp = require "compiler"
local loader = require "loader"

local src = ...
if not src or src == "" then
  print("uso: luajit runner.lua \"codigo lua\"")
  return
end

local cipher = comp.obfuscate(src)
-- opcional: ver el cipher como hex
io.write("cipher(hex): ")
for i = 1, #cipher do io.write(string.format("%02x", string.byte(cipher, i))) end
print()
print("--- salida ---")
loader.execute(cipher)
