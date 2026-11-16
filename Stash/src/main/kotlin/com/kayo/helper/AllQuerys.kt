package com.kayo.helper

val randomNumber = (1_000_000..9_999_999).random()
fun getAllScenes(page: Int, query: String = ""): String {
    return $$"""{
  "operationName": "FindScenes",
  "variables": {
    "filter": {
      "q": "$$query",
      "page": $$page,
      "per_page": 40,
      "sort": "date",
      "direction": "DESC"
    },
    "scene_filter": {},
    "scene_ids": null
  },
  "query": "query FindScenes($filter: FindFilterType, $scene_filter: SceneFilterType, $scene_ids: [Int!]) {\n  findScenes(filter: $filter, scene_filter: $scene_filter, scene_ids: $scene_ids) {\n    count\n    filesize\n    duration\n    scenes {\n      ...SlimSceneData\n      __typename\n    }\n    __typename\n  }\n}\n\nfragment SlimSceneData on Scene {\n  id\n  title\n  date\n  o_counter\n  rating100\n  files {\n    ...VideoFileData\n    __typename\n  }\n  paths {\n    screenshot\n    preview\n    stream\n    webp\n    sprite\n    __typename\n  }\n  tags { id name __typename }\n  performers { id name image_path __typename }\n  studio { id name image_path __typename }\n  __typename\n}\n\nfragment VideoFileData on VideoFile {\n  id\n  path\n  width\n  height\n  duration\n  video_codec\n  audio_codec\n  size\n  __typename\n}"
}
""".trimIndent()
}

fun getMostViews(page: Int, query: String = ""): String {
    return $$"""{
  "operationName": "FindScenes",
  "variables": {
    "filter": {
      "q": "$$query",
      "page": $$page,
      "per_page": 40,
      "sort": "play_count",
      "direction": "DESC"
    },
    "scene_filter": {},
    "scene_ids": null
  },
  "query": "query FindScenes($filter: FindFilterType, $scene_filter: SceneFilterType, $scene_ids: [Int!]) {\n  findScenes(filter: $filter, scene_filter: $scene_filter, scene_ids: $scene_ids) {\n    count\n    filesize\n    duration\n    scenes {\n      ...SlimSceneData\n      __typename\n    }\n    __typename\n  }\n}\n\nfragment SlimSceneData on Scene {\n  id\n  title\n  date\n  o_counter\n  rating100\n  files {\n    ...VideoFileData\n    __typename\n  }\n  paths {\n    screenshot\n    preview\n    stream\n    webp\n    sprite\n    __typename\n  }\n  tags { id name __typename }\n  performers { id name image_path __typename }\n  studio { id name image_path __typename }\n  __typename\n}\n\nfragment VideoFileData on VideoFile {\n  id\n  path\n  width\n  height\n  duration\n  video_codec\n  audio_codec\n  size\n  __typename\n}"
}
""".trimIndent()
}

fun getJavAtScenes(page: Int): String {
    return $$"""{
  "operationName": "FindScenes",
  "variables": {
    "filter": {
      "q": "",
      "page": $$page,
      "per_page": 40,
      "direction": "DESC"
    },
    "scene_filter": {
      "tags": {
        "value": [
          "797"
        ],
        "excludes": [],
        "modifier": "INCLUDES_ALL",
        "depth": 0
      }
    },
    
    "scene_ids": null
  },
  "query": "query FindScenes($filter: FindFilterType, $scene_filter: SceneFilterType, $scene_ids: [Int!]) {\n  findScenes(filter: $filter, scene_filter: $scene_filter, scene_ids: $scene_ids) {\n    count\n    filesize\n    duration\n    scenes {\n      ...SlimSceneData\n      __typename\n    }\n    __typename\n  }\n}\n\nfragment SlimSceneData on Scene {\n  id\n  title\n  date\n  o_counter\n  rating100\n  files {\n    ...VideoFileData\n    __typename\n  }\n  paths {\n    screenshot\n    preview\n    stream\n    webp\n    sprite\n    __typename\n  }\n  tags { id name __typename }\n  performers { id name image_path __typename }\n  studio { id name image_path __typename }\n  __typename\n}\n\nfragment VideoFileData on VideoFile {\n  id\n  path\n  width\n  height\n  duration\n  video_codec\n  audio_codec\n  size\n  __typename\n}"
}
""".trimIndent()
}

fun getRanAtScenes(page: Int): String {
    return $$"""{
  "operationName": "FindScenes",
  "variables": {
    "filter": {
      "q": "",
      "page": $$page,
      "per_page": 40,
      "sort": "random_$$randomNumber",
      "direction": "DESC"
    },
    "scene_filter": {},
    "scene_ids": null
  },
  "query": "query FindScenes($filter: FindFilterType, $scene_filter: SceneFilterType, $scene_ids: [Int!]) {\n  findScenes(filter: $filter, scene_filter: $scene_filter, scene_ids: $scene_ids) {\n    count\n    filesize\n    duration\n    scenes {\n      ...SlimSceneData\n      __typename\n    }\n    __typename\n  }\n}\n\nfragment SlimSceneData on Scene {\n  id\n  title\n  date\n  o_counter\n  rating100\n  files {\n    ...VideoFileData\n    __typename\n  }\n  paths {\n    screenshot\n    preview\n    stream\n    webp\n    sprite\n    __typename\n  }\n  tags { id name __typename }\n  performers { id name image_path __typename }\n  studio { id name image_path __typename }\n  __typename\n}\n\nfragment VideoFileData on VideoFile {\n  id\n  path\n  width\n  height\n  duration\n  video_codec\n  audio_codec\n  size\n  __typename\n}"
}
""".trimIndent()
}


fun getUpdatedAtScenes(page: Int, query: String = ""): String {
    return $$"""{
  "operationName": "FindScenes",
  "variables": {
    "filter": {
      "q": "$$query",
      "page": $$page,
      "per_page": 40,
      "sort": "updated_at",
      "direction": "DESC"
    },
    "scene_filter": {},
    "scene_ids": null
  },
  "query": "query FindScenes($filter: FindFilterType, $scene_filter: SceneFilterType, $scene_ids: [Int!]) {\n  findScenes(filter: $filter, scene_filter: $scene_filter, scene_ids: $scene_ids) {\n    count\n    filesize\n    duration\n    scenes {\n      ...SlimSceneData\n      __typename\n    }\n    __typename\n  }\n}\n\nfragment SlimSceneData on Scene {\n  id\n  title\n  date\n  o_counter\n  rating100\n  files {\n    ...VideoFileData\n    __typename\n  }\n  paths {\n    screenshot\n    preview\n    stream\n    webp\n    sprite\n    __typename\n  }\n  tags { id name __typename }\n  performers { id name image_path __typename }\n  studio { id name image_path __typename }\n  __typename\n}\n\nfragment VideoFileData on VideoFile {\n  id\n  path\n  width\n  height\n  duration\n  video_codec\n  audio_codec\n  size\n  __typename\n}"
}
""".trimIndent()
}

fun getFavAtScenes(page: Int): String {
    return $$"""{
  "operationName": "FindScenes",
  "variables": {
    "filter": {
      "q": "",
      "page": $$page,
      "per_page": 40,
      "direction": "DESC"
    },
    "scene_filter": {
      "tags": {
        "value": [
          "731"
        ],
        "excludes": [],
        "modifier": "INCLUDES_ALL",
        "depth": 0
      }
    },
    
    "scene_ids": null
  },
  "query": "query FindScenes($filter: FindFilterType, $scene_filter: SceneFilterType, $scene_ids: [Int!]) {\n  findScenes(filter: $filter, scene_filter: $scene_filter, scene_ids: $scene_ids) {\n    count\n    filesize\n    duration\n    scenes {\n      ...SlimSceneData\n      __typename\n    }\n    __typename\n  }\n}\n\nfragment SlimSceneData on Scene {\n  id\n  title\n  date\n  o_counter\n  rating100\n  files {\n    ...VideoFileData\n    __typename\n  }\n  paths {\n    screenshot\n    preview\n    stream\n    webp\n    sprite\n    __typename\n  }\n  tags { id name __typename }\n  performers { id name image_path __typename }\n  studio { id name image_path __typename }\n  __typename\n}\n\nfragment VideoFileData on VideoFile {\n  id\n  path\n  width\n  height\n  duration\n  video_codec\n  audio_codec\n  size\n  __typename\n}"
}
""".trimIndent()
}

fun findSceneById(id: Int): String {
    return $$"""{
  "operationName": "FindScene",
  "variables": {
    "id": $$id
  },
  "query": "query FindScene($id: ID!) {\n  findScene(id: $id) {\n    id\n    title\n    code\n    details\n    urls\n    date\n    rating100\n    o_counter\n    organized\n    interactive\n    interactive_speed\n    captions { language_code caption_type }\n    created_at\n    updated_at\n    resume_time\n    last_played_at\n    play_duration\n    play_count\n    play_history\n    o_history\n    files {\n      id\n      path\n      size\n      mod_time\n      duration\n      video_codec\n      audio_codec\n      width\n      height\n      frame_rate\n      bit_rate\n      fingerprints { type value }\n    }\n    paths {\n      screenshot\n      preview\n      stream\n      webp\n      vtt\n      sprite\n      funscript\n      interactive_heatmap\n      caption\n    }\n    scene_markers {\n      id\n      title\n      seconds\n      end_seconds\n      stream\n      preview\n      screenshot\n      primary_tag { id name }\n      tags { id name }\n    }\n    galleries {\n      id\n      title\n      image_count\n      paths { cover preview }\n    }\n    studio {\n      id\n      name\n      image_path\n    }\n    performers {\n      id\n      name\n      disambiguation\n      gender\n      favorite\n      image_path\n      rating100\n    }\n    tags { id name }\n    stash_ids { endpoint stash_id updated_at }\n    sceneStreams { url mime_type label }\n  }\n}"
}
"""
}

fun updateViewCount(id: Int): String {
    return $$"""{ "operationName":"SceneAddPlay", 
        "variables":{ 
            "id":$$id 
                    }, 
        "query":"mutation SceneAddPlay($id: ID!, $times: [Timestamp!]) {\n  sceneAddPlay(id: $id, times: $times) {\n    count\n    history\n    __typename\n  }\n}" 
    }"""
}


fun findPerformer(page: Int, query: String = "",sort : String = "name"): String {
    val sort1 = if(sort == "random"){"random_$randomNumber"} else {
        sort
    }
    return $$"""{
      "operationName": "FindPerformers",
      "variables": {
        "filter": {
          "q": "$$query",
          "page": $$page,
          "per_page": 40,
          "sort": "$$sort1",
          "direction": "ASC"
        },
        "performer_filter": {}
      },
      "query": "query FindPerformers($filter: FindFilterType, $performer_filter: PerformerFilterType, $performer_ids: [Int!]) {\n  findPerformers(\n    filter: $filter\n    performer_filter: $performer_filter\n    performer_ids: $performer_ids\n  ) {\n    count\n    performers {\n      ...PerformerData\n      __typename\n    }\n    __typename\n  }\n}\n\nfragment PerformerData on Performer {\n  id\n  name\n  disambiguation\n  urls\n  gender\n  birthdate\n  ethnicity\n  country\n  eye_color\n  height_cm\n  measurements\n  fake_tits\n  penis_length\n  circumcised\n  career_start\n  career_end\n  tattoos\n  piercings\n  alias_list\n  favorite\n  ignore_auto_tag\n  image_path\n  scene_count\n  image_count\n  gallery_count\n  group_count\n  performer_count\n  o_counter\n  tags {\n    ...SlimTagData\n    __typename\n  }\n  stash_ids {\n    stash_id\n    endpoint\n    updated_at\n    __typename\n  }\n  rating100\n  details\n  death_date\n  hair_color\n  weight\n  custom_fields\n  __typename\n}\n\nfragment SlimTagData on Tag {\n  id\n  name\n  sort_name\n  aliases\n  image_path\n  parent_count\n  child_count\n  stash_ids {\n    endpoint\n    stash_id\n    updated_at\n    __typename\n  }\n  __typename\n}"
    }""".trimIndent()
}

fun getPerformerById(id: String): String {
    return $$"""{
      "operationName": "FindPerformer",
      "variables": {
        "id": "$$id"
      },
      "query": "query FindPerformer($id: ID!) {\n  findPerformer(id: $id) {\n    ...PerformerData\n    __typename\n  }\n}\n\nfragment PerformerData on Performer {\n  id\n  name\n  disambiguation\n  urls\n  gender\n  birthdate\n  ethnicity\n  country\n  eye_color\n  height_cm\n  measurements\n  fake_tits\n  penis_length\n  circumcised\n  career_start\n  career_end\n  tattoos\n  piercings\n  alias_list\n  favorite\n  ignore_auto_tag\n  image_path\n  scene_count\n  image_count\n  gallery_count\n  group_count\n  performer_count\n  o_counter\n  tags {\n    id name\n    __typename\n  }\n  stash_ids {\n    stash_id endpoint updated_at\n    __typename\n  }\n  rating100\n  details\n  death_date\n  hair_color\n  weight\n  custom_fields\n  __typename\n}"
    }""".trimIndent()
}

fun getPerformerScenes(page: Int, performerId: String): String {
    return $$"""{
      "operationName": "FindScenes",
      "variables": {
        "filter": {
          "q": "",
          "page": $$page,
          "per_page": 40,
          "sort": "date",
          "direction": "DESC"
        },
        "scene_filter": {
          "performers": {
            "value": ["$$performerId"],
            "excludes": [],
            "modifier": "INCLUDES_ALL"
          }
        },
        "scene_ids": null
      },
      "query": "query FindScenes($filter: FindFilterType, $scene_filter: SceneFilterType, $scene_ids: [Int!]) {\n  findScenes(filter: $filter, scene_filter: $scene_filter, scene_ids: $scene_ids) {\n    count\n    filesize\n    duration\n    scenes {\n      id\n      title\n      code\n      details\n      director\n      urls\n      date\n      rating100\n      o_counter\n      resume_time\n      play_duration\n      play_count\n      files {\n        id\n        path\n        size\n        mod_time\n        duration\n        video_codec\n        audio_codec\n        width\n        height\n        frame_rate\n        bit_rate\n      }\n      paths {\n        screenshot\n        preview\n        stream\n        webp\n        sprite\n      }\n      studio {\n        id name image_path\n      }\n      tags {\n        id name\n      }\n      performers {\n        id name image_path\n      }\n    }\n  }\n}"
    }""".trimIndent()
}