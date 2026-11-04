package com.mmorpg.assets;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
/** Prevent a single invalid imported animation from blocking the entire client resource reload. */
class ImportedAnimationAssetsTest {
 private static void ids(JsonArray parts,Set<String> ids){for(var value:parts){var p=value.getAsJsonObject();ids.add(p.get("uuid").getAsString());if(p.has("children"))ids(p.getAsJsonArray("children"),ids);}}
 @Test void everyAnimationTargetsAnExistingPart() throws Exception {
  var errors=new ArrayList<String>();int count=0;
  try(var files=Files.walk(Path.of("src/main/resources/assets/mmorpg/bbmodels"))){
   for(var p:files.filter(p->p.toString().endsWith(".json")).toList()){
    var data=JsonParser.parseString(Files.readString(p)).getAsJsonObject();if(!data.has("parts"))continue;var bones=new HashSet<String>();ids(data.getAsJsonArray("parts"),bones);count++;
    for(var value:data.getAsJsonArray("animations")){var a=value.getAsJsonObject();for(String line:a.get("tracks").getAsString().split("\\R"))if(!line.isBlank()&&!bones.contains(line.split("\\|",2)[0]))errors.add(p.getFileName()+" / "+a.get("name")+" / "+line.split("\\|",2)[0]);}
   }
  }
  assertTrue(count>50,"Model scan must include the shipped catalogue");assertTrue(errors.isEmpty(),String.join("\n",errors));
 }
}
