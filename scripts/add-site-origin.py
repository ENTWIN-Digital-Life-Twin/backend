from pathlib import Path
p = Path("/home/ubuntu/backend/.env")
lines = p.read_text().splitlines()
origin = "https://entwin.3utilities.com"
out = []
found = False
for line in lines:
    if line.startswith("CORS_ALLOWED_ORIGINS="):
        found = True
        value = line.split("=", 1)[1]
        parts = [item.strip() for item in value.split(",") if item.strip()]
        if origin not in parts:
            parts.append(origin)
        line = "CORS_ALLOWED_ORIGINS=" + ",".join(parts)
    out.append(line)
if not found:
    out.append("CORS_ALLOWED_ORIGINS=" + origin)
p.write_text("\n".join(out) + "\n")
print("ok")
