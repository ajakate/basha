import psycopg2
from IPython import embed
import genanki
import uuid
import ffmpeg
import os
import sys
import re
import traceback
from urllib.parse import urlparse

try:
  list_id = sys.argv[1]
  db_url = sys.argv[2]

  if db_url.startswith("postgres"):
    dbc = urlparse(db_url)
    host = dbc.hostname
    user = dbc.username
    password = dbc.password
    database = dbc.path[1:]
    if dbc.port:
      port = dbc.port
    else:
      port = 5432
  else:
    matches = re.search(r'jdbc:postgresql:\/\/(.*):(\d+)\/(.*)\?user=(.*)&password=(.*)', db_url)

    user = matches[4]
    password = matches[5]
    host = matches[1]
    port = matches[2]
    database = matches[3]


  query = f"""
  select
  t.source_text,
  t.target_text,
  t.target_text_roman,
  t.audio,
  l.name
  from translations t
  join lists l on l.id=t.list_id
  where l.id='{list_id}'
  and
  (t.target_text is not null or t.target_text_roman is not null)
  order by t.list_index asc;
  """

  conn = psycopg2.connect(
      host=host,
      database=database,
      port=port,
      user=user,
      password=password)

  cur = conn.cursor()
  cur.execute(query)
  rows = cur.fetchall()
  cur.close()
  conn.close()

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

  list_name = rows[0][4]
  my_deck = genanki.Deck(2059400110, f"Basha - {list_name}")

  my_package = genanki.Package(my_deck)

  media_ids = []
  for r in rows:
      stream = r[3]
      if r[1] == None:
          source = ""
      else:
          source = r[1]
      media_id = str(uuid.uuid4())
      my_fields = [r[0], source, r[2]]
      if stream is not None:
        with open(f'temp_media/{media_id}.ogg', 'wb') as f:
            f.write(stream)
        ffmpeg.input(f'temp_media/{media_id}.ogg').output(f'temp_media/{media_id}.mp3').run()
        media_ids.append(f'temp_media/{media_id}.mp3')
        my_fields.append(f"[sound:{media_id}.mp3]")
      my_note = genanki.Note(
          model=my_model,
          fields=[r[0], source, r[2], f"[sound:{media_id}.mp3]"]
          )
      my_deck.add_note(my_note)

  my_package.media_files = media_ids
  my_package.write_to_file(f'temp_decks/{list_id}.apkg')
except Exception as err:
  with open(f'temp_decks/{list_id}.fail', 'w') as f:
    traceback.print_exc(file=f)
finally:
  try:
    os.system(f"rm temp_decks/{list_id}.pending")
  except:
    None
  try:
    os.system("rm temp_media/*.mp3")
  except:
    None
  try:
    os.system("rm temp_media/*.ogg")
  except:
    None
