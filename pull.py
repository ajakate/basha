import psycopg2
from IPython import embed
import genanki
import uuid
import ffmpeg
import os

query = """
select
source_text,
target_text,
target_text_roman,
audio
from translations t
where list_id='ede6f320-5e5f-4040-953b-123f43935bd0'
order by list_index asc;
"""

conn = psycopg2.connect(
    host="localhost",
    database="dev",
    user="basha_user",
    password="basha_password")

cur = conn.cursor()
cur.execute(query)
rows = cur.fetchall()
cur.close()

my_model = genanki.Model(
  1607392319,
  'Simple Model',
  fields=[
    {'name': 'Question'},
    {'name': 'Answer'},
    {'name': 'Answer2'},
    {'name': 'MyMedia'},
  ],
  templates=[
    {
      'name': 'Card 1',
      'qfmt': '{{Question}}',
      'afmt': '{{FrontSide}}<hr id="answer">{{Answer}}<br>{{Answer2}}<br>{{MyMedia}}',
    },
  ])

my_deck = genanki.Deck(2059400110,'Test List heroku')

my_package = genanki.Package(my_deck)

media_ids = []
for r in rows:
    stream = r[3]
    if r[1] == None:
        source = ""
    else:
        source = r[1]
    media_id = str(uuid.uuid4())
    with open(f'temp_media/{media_id}.ogg', 'wb') as f:
        f.write(stream)
    ffmpeg.input(f'temp_media/{media_id}.ogg').output(f'temp_media/{media_id}.mp3').run()
    my_note = genanki.Note(
        model=my_model,
        fields=[r[0], source, r[2], f"[sound:{media_id}.mp3]"]
        )
    my_deck.add_note(my_note)
    media_ids.append(f'temp_media/{media_id}.mp3')

my_package.media_files = media_ids
my_package.write_to_file('output.apkg')
os.system("rm temp_media/*.mp3")
os.system("rm temp_media/*.ogg")
