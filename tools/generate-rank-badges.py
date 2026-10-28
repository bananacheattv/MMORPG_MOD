from pathlib import Path
import json
from PIL import Image,ImageDraw,ImageFont
ROOT=Path(__file__).resolve().parents[1];RES=ROOT/'src/main/resources/assets/mmorpg';font=ImageFont.truetype(str(RES/'font/nunito.ttf'),18)
labels=['Aelorien','Éclaireur','Gardien','Arcaniste','Élu','Fondateur','Administrateur','Modérateur','Builder'];colors=['#C3B4DF','#74CF96','#79C9F2','#BA8CFF','#F4CA69','#FF5555','#FF5555','#55AFFF','#58DE87'];providers=[]
for i,(name,color) in enumerate(zip(labels,colors)):
 text=name.upper();width=int(font.getlength(text))+36;img=Image.new('RGBA',(width,28));draw=ImageDraw.Draw(img);draw.rounded_rectangle((0,1,width-1,26),radius=7,fill='#20132BEF',outline=color,width=2);draw.polygon([(11,6),(16,13),(11,21),(6,13)],fill=color);draw.line([(11,8),(11,18)],fill='#FFF2D3',width=1);draw.text((22,3),text,font=font,fill=color,stroke_width=0);target=RES/'textures/gui/ranks'/f'rank_{i}.png';target.parent.mkdir(parents=True,exist_ok=True);img.save(target);providers.append({'type':'bitmap','file':f'mmorpg:gui/ranks/rank_{i}.png','ascent':8,'height':10,'chars':[chr(0xE500+i)]})
(RES/'font/rank_badges.json').write_text(json.dumps({'providers':providers},indent=2),encoding='utf-8')
