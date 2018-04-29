// CodeMirror, copyright (c) by Marijn Haverbeke and others
// Distributed under an MIT license: http://codemirror.net/LICENSE

(function(mod) {
	if (typeof exports == "object" && typeof module == "object") // CommonJS
		mod(require("../../lib/codemirror"));
	else if (typeof define == "function" && define.amd) // AMD
		define(["../../lib/codemirror"], mod);
	else // Plain browser env
		mod(CodeMirror);
})(function(CodeMirror) {
	"use strict";
	function containsAll(str,strarr){
		if(strarr){
			for(var i=0;i<strarr.length;i++){
				if(str.indexOf( strarr[i] )==-1){
					return false;
				}
			}
		}
		return true;
	}

	CodeMirror.registerHelper("hint", "myHint", function(editor, options) {
		var myOpts = editor.options.myHint;
		var validChars = myOpts.validChars;
		var splitDelim = myOpts.splitDelim;
		var properties = myOpts.properties;
		
		var list = [];
		var cur = editor.getCursor(), line = editor.getLine(cur.line);
		var end = cur.ch, start = end;
		while (start && validChars.test(line.charAt(start - 1))) --start;
		if(start<end){
			line = line.slice(start,end);
			var lines = line.split( splitDelim );
			for(var i=0; i<properties.length; i++){
				if( containsAll( properties[i], lines) )list.push(properties[i]);
			}
		}
		if(start==end){
			//show all
			list = properties;
		}
		
		return {list: list, from: CodeMirror.Pos(cur.line, start), to: CodeMirror.Pos(cur.line, end)};
	});
  
	CodeMirror.commands.autocomplete = function(cm) {
		CodeMirror.showHint(cm, CodeMirror.hint.myHint);
	};
});
