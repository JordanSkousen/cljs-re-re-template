import fetch from "node-fetch";
import fs from "fs";

const albums = {
  "43OpbkiiIxJO8ktIB777Nn": "4hDok0OAJd57SGIT8xuWJH", //Fearless
  "5EpMjweRD573ASl7uNiHym": "5AEDGbliTTfjOB8TSm1sxt", //Speak Now
  "1KlU96Hw9nlvqpBPlSqcTV": "6kZ42qRrzov54LcAk4onW9", //Red
  "34OkZVpuzBa9y40DCy0LPR": "1o59UpKw81iHR0HPiSkJR0", //1989
};
const at = "Bearer BQB2GMFbI1NLQhSVzWS5gJa75Qd8-8FOvcVd5Tp9u5hwTNn55IS4N5EL4uugjnJQG8cYEY16g_86FZLlKaXaD9CCZFtv1IIYJFO6H9w9J53eCReOiXUe6L0Jfl6GDKcUYmgL09zeF46sPVbnOemTb70Ua6dP2kD5bT1EuX1fYIlV_jkWIKJi";
let out = [];

for (const ogId of Object.keys(albums)) {
  const tvId = albums[ogId];
  console.log(`Getting OG album "${ogId}"...`);
  const ogTracks = (await (await fetch(`https://api.spotify.com/v1/albums/${ogId}`, {headers: {Authorization: at}})).json())?.tracks?.items;
  console.log(`Getting TV album "${tvId}"...`);
  const tvTracks = (await (await fetch(`https://api.spotify.com/v1/albums/${tvId}`, {headers: {Authorization: at}})).json())?.tracks?.items;
  //console.log(ogTracks.map(i => i.name));
  //console.log(tvTracks.map(i => i.name));
  let album = {ogTracks: ogTracks.map(i => ({id: i.id, name: i.name})), tvTracks: tvTracks.map(i => ({id: i.id, name: i.name})), idMap: {}};
  for (const ogTrack of ogTracks) {
    const tvTrack = tvTracks.filter(i => i.name.replace(" (Taylor’s Version)", "").replace(" (Taylor's Version)", "") === ogTrack.name)[0];
    if (!tvTrack) {
      console.error(`Failed to find Taylor's Version of track: "${ogTrack.name}"!`);
      continue;
    }
    album.idMap[ogTrack.id] = tvTrack.id;
  }
  out.push(album);
}

fs.writeFileSync("out.json", JSON.stringify(out));