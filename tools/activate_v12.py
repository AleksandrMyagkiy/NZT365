from pathlib import Path

product = Path('app/src/main/java/com/nzt365/app/V7Product.kt')
s = product.read_text(encoding='utf-8')
s = s.replace('Intent(context, V7WorkoutActivity::class.java)', 'Intent(context, V12WorkoutActivity::class.java)')
product.write_text(s, encoding='utf-8')

manifest = Path('app/src/main/AndroidManifest.xml')
m = manifest.read_text(encoding='utf-8')
needle = '        <activity android:name=".V7WorkoutActivity" android:exported="false" android:screenOrientation="portrait" android:windowSoftInputMode="adjustResize" />\n'
entry = '        <activity android:name=".V12WorkoutActivity" android:exported="false" android:screenOrientation="portrait" android:windowSoftInputMode="adjustResize" />\n'
if entry not in m:
    if needle not in m:
        raise SystemExit('V7 activity anchor not found')
    m = m.replace(needle, entry + needle, 1)
manifest.write_text(m, encoding='utf-8')
