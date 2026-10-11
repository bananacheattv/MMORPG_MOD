@echo off
cd /d "%~dp0"
python -m pip install -q -r requirements.txt
if not exist AutoPeche_Detection.zip python gen_texture_pack.py
python autopeche.py
pause
