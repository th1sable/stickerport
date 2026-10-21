import json, subprocess
from pathlib import Path
root=Path(__file__).resolve().parents[1]
devices=json.loads(subprocess.check_output(['xcrun','simctl','list','devices','available','-j']))['devices']
device=next(d for runtime,items in reversed(list(devices.items())) if 'iOS' in runtime for d in items if d['name'].startswith('iPhone'))
command=['xcodebuild','-project','ios/StickerPort.xcodeproj','-scheme','StickerPort','-destination','platform=iOS Simulator,id='+device['udid'],'-resultBundlePath','ios/TestResults.xcresult','CODE_SIGNING_ALLOWED=NO','test']
raise SystemExit(subprocess.call(command,cwd=root))
