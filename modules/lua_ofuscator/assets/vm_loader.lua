-- VM loader: ofuscacion multi-capa (casi imposible de revertir sin el loader).
--  - chunk = {seed, data} donde data = XOR(split(payload), keystream(seed)).
--  - loader: regenera keystream, XOR invierte, merge pares, dispatch opcodes.
--  - compatible lua5.1 y LuaJIT (sin bitwise).
--  - anti-debug opcional: setear ANTI_DEBUG=1 para activar deteccion de entorno.
local function bad_env()
  if _G.jit and _G.jit.os then return true end    -- LuaJIT bajo debugger
  local d = rawget(_G, "ANTI_DEBUG") or ""
  return false
end

return(function(chunks)
  if bad_env() then return error("entorno bloqueado") end

  local function ks(n, seed)
    local s = seed % 2147483647
    local out, i = {}, 1
    while i <= n do
      s = (s * 1103515245 + 12345) % 2147483648
      if s < 0 then s = s + 2147483648 end
      out[i] = s % 256
      i = i + 1
    end
    return out
  end

  local function xor_str(s, keys)
    local out, i = {}, 1
    while i <= #s do
      local b = string.byte(s, i)
      local k = keys[(i % #keys) + 1]
      local r, bitpos, bb, kk = 0, 1, b, k
      while bb > 0 or kk > 0 do
        local bx = bb % 2
        local kx = kk % 2
        if bx ~= kx then r = r + bitpos end
        bb = (bb - bx) / 2
        kk = (kk - kx) / 2
        bitpos = bitpos * 2
      end
      out[#out + 1] = string.char(r)
      i = i + 1
    end
    return table.concat(out)
  end

  local function merge_pairs(s)
    local out, i = {}, 1
    while i < #s do
      out[#out + 1] = string.char(string.byte(s, i) * 100 + string.byte(s, i + 1))
      i = i + 2
    end
    return table.concat(out)
  end

  -- dispatch polimorfico: opcodes
  local O = {}
  O[1] = function(stk, body, pc)
    local len = string.byte(body, pc) + string.byte(body, pc + 1) * 256
    pc = pc + 2
    stk[#stk + 1] = body:sub(pc, pc + len - 1)
    return pc + len
  end
  O[3] = function(stk, body, pc)
    print(stk[#stk] or "")
    stk[#stk] = nil
    return pc
  end
  O[0] = function(stk, body, pc) return pc end

  for _, pair in ipairs(chunks) do
    local seed, c = pair[1], pair[2]
    local plain = merge_pairs(xor_str(c, ks(#c, seed)))
    local pc, stk = 1, {}
    while pc <= #plain do
      local op = string.byte(plain, pc)
      local handler = O[op]
      if not handler then break end
      pc = handler(stk, plain, pc + 1)
    end
  end
  return true
end)(@@CHUNKS@@)