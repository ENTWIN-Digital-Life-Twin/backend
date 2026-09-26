from pathlib import Path
incoming = {}
for line in Path("/tmp/entwin-mail.env").read_text().splitlines():
    if "=" in line:
        key, value = line.split("=", 1)
        incoming[key] = value
path = Path("/home/ubuntu/backend/.env")
lines = path.read_text().splitlines()
seen = set()
out = []
for line in lines:
    key = line.split("=", 1)[0] if "=" in line and not line.startswith("#") else None
    if key in incoming:
        out.append(f"{key}={incoming[key]}")
        seen.add(key)
    else:
        out.append(line)
for key, value in incoming.items():
    if key not in seen:
        out.append(f"{key}={value}")
path.write_text("\n".join(out) + "\n")
print("merged", len(incoming))
