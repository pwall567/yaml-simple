package net.pwall.yaml

import net.pwall.json.JSONException
import net.pwall.yaml.parser.Line

class YAMLException(message: String, line: Line, e: Exception? = null) :
        JSONException("$message at ${line.lineNumber}:${line.index + 1}", e)
