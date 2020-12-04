$(document).ready(function(){
	getProcessed();
});

function getProcessed() {
	$.ajax({
		url : "/rest/getFiles",
		data : {},
		success : function(res) {
			var grid = $('.files-container');
			$(grid).append("<div class='item'>Date Processed</div>");
			$(grid).append("<div class='item'>File Name</div>");
			var date;
			var name;
			$(res).each(function() {
				date = $("<div class='item'></div>");
				$(date).text(moment(this.dateProcessed).format("DD-MM-YYYY HH:mm:ss"));
				name = $("<div class='item'></div>");
				$(name).text(this.fileName);
				$(grid).append(date);
				$(grid).append(name);
			});
		},
		error: function(e1, e2, e3) {
			alert(e1 + "\r\n" + e2 + "\r\n" + e3);
		}
	})
}

const omdb = 'https://www.omdbapi.com/';
const omdbKey = 'd6985468';
const omdbData = {
	"apikey": omdbKey,
	"s" : ""
};

function searchOMDB(search, callback) {
	omdbData.s = search;
	
	$.get(omdb, omdbData, function(data) {
		var list = [];
		var title;
		var img;
		var cont;
		console.log(data);
		$(data.Search).each(function(){
			title = $('<div class="imdb-title"></div>');
			$(title).text(this.Title + " " + this.Year);
			img = $('<img src="' + this.Poster + '" class="imdb-img"/>');
			cont = $('<div class="imdb-container"></div>');
			$(cont).append(img);
			$(cont).append(title);
			list.push({"label" : cont, "value" : this.imdbID});
		});
		console.log(list);
		if (callback) {
			callback(list);
		}
	});
}

var searchTimeout;
$(document).on("keyup", "#omdb-search", function (){
	var item = this;
	var data = $(item).val();
	
	if (searchTimeout) {
		clearTimeout(searchTimeout);
		searchTimeout = null;
	}
	
	searchTimeout = setTimeout(function() {
		try {
			$(item).autocomplete("destroy");
		}
		catch {
			console.error("Called Destroy before Initializing autocomplete");
		}
		if (data.length > 2) {
			searchOMDB(data, function(list){
				console.log("Callback");
				$(item).autocomplete({
					source: list,
					appendTo: document.body,
					html: true
				});
				$(item).autocomplete("search", data);
			})
		}
		
	}, 1500);
});


/*
 * jQuery UI Autocomplete HTML Extension
 *
 * Copyright 2010, Scott González (http://scottgonzalez.com)
 * Dual licensed under the MIT or GPL Version 2 licenses.
 *
 * http://github.com/scottgonzalez/jquery-ui-extensions
 */
(function( $ ) {

var proto = $.ui.autocomplete.prototype,
	initSource = proto._initSource;

function filter( array, term ) {
	var matcher = new RegExp( $.ui.autocomplete.escapeRegex(term), "i" );
	return $.grep( array, function(value) {
		return matcher.test( $( "<div>" ).html( value.label || value.value || value ).text() );
	});
}

$.extend( proto, {
	_initSource: function() {
		console.log("HTML :" + this.options.html);
		if ( this.options.html && $.isArray(this.options.source) ) {
			this.source = function( request, response ) {
				response( filter( this.options.source, request.term ) );
			};
		} else {
			initSource.call( this );
		}
	},

	_renderItem: function( ul, item) {
		console.log(item);
		console.log("HTML :" + this.options.html);
		return $( "<li></li>" )
			.data( "item.autocomplete", item )
			.append( $( "<a></a>" )[ this.options.html ? "html" : "text" ]( item.label ) )
			.appendTo( ul );
	}
});

})( jQuery );


