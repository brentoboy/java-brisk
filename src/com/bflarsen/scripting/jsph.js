(function () { // modified slightly from github version to target nashorn rather than browser / node.js
	var nullTemplate = function() { return ''; };

	var jsph = {
		compile: function compile(templateString) {
			if (!templateString) {
				return nullTemplate;
			}

			templateString = "?>" + templateString + "<?";

			var inHtml = /[\?\%]>=?[\s\S]*?<[\?\%]/mgi;
			var inJs = /<[\?\%]=?[\s\S]*?<?[\?\%]>/mgi;

			var functionBody = "\
(function(jsph) { \n\
	return function(vars) { \n\
	    var model = vars; \n\
	    for(var varName in model) { \n\
	        try { eval('var ' + varName + '=model[varName];') } \n\
	        catch(ex){} \n\
        } \n\
		return (function() { \n\
			var o = \"\";\n";

			htmlMatch = inHtml.exec(templateString);
			jsMatch = inJs.exec(templateString);

			while (htmlMatch !== null || jsMatch !== null)
			{
				var matchStartsAt, matchLen;

				if (htmlMatch !== null)
				{
					matchStartsAt = htmlMatch['index'];
					matchLen = htmlMatch[0].length;
					htmlMatch = htmlMatch[0].substring(2, matchLen - 2);

					if (htmlMatch !== "") {
						functionBody += "o += " + JSON.stringify(htmlMatch) + ";\n";
					}

					htmlMatch = inHtml.exec(templateString);
				}

				if (jsMatch !== null)
				{
					jsMatch = jsMatch[0].substring(2, jsMatch[0].length -2);

					if (/^=/.test(jsMatch)) {
						functionBody += "o += (" + jsMatch.substring(1).trim() + ");\n";
					}
					else {
						functionBody += jsMatch + "\n";
					}

					jsMatch = inJs.exec(templateString);
				}
				else if (matchStartsAt && matchLen && (matchStartsAt + matchLen < templateString.length)) {
					// they left off the closing tag, treat the rest like a js script
					jsMatch = templateString.substring(matchStartsAt + matchLen, templateString.length - 2);
					functionBody += jsMatch + "\n";
					jsMatch = null;
				}
			}

			functionBody += "\
			 return o;\n\
		}).call(vars); \n\
	} \n\
})(jsph)";
			try {
				var fn = eval(functionBody);
				return fn;
			} catch(err) {
				throw err.message + "\n" + functionBody;
			}
		},

		render: function render(templateString, vars) {
			var renderer = jsph.compile(templateString);
			return renderer(vars);
		},

		createModelFactory: function createModelFactory(code) {
            if (code === null || code == undefined) {
                return function() { return null; };
            }
            if (typeof code != "string") {
                code = JSON.stringify(code);
            }
            return function() { eval("var inst = " + code + ";"); return inst; }
		},
	}
    window.jsph = jsph;
}());