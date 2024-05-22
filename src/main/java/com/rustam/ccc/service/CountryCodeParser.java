package com.rustam.ccc.service;

import com.rustam.ccc.domain.CountryCode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CountryCodeParser {
	private final static String URL_TO_PARSE = "https://en.wikipedia.org/wiki/List_of_country_calling_codes#Alphabetical_order";
	private final static ClassPathResource SAVED_FILE = new ClassPathResource("saved_page.html");
	private final static String CODE_SEPARATOR = ",";
	private final static String EXTRA_START = "(";
	private final static String EXTRA_END = ")";
	private final Logger log = LoggerFactory.getLogger(CountryCodeParser.class);
	private final CountryCodeService service;

	public CountryCodeParser(CountryCodeService service) {
		this.service = service;
	}

	/** Fetch online page and parse codes from it, saving to database.
	 *
	 * @throws IOException in case when even fallback method isn't working
	 */
	public void parse() throws IOException {
		Document doc = null;
		try {
			doc = Jsoup.connect(URL_TO_PARSE).get();
			log.info("Fetching online page from {} with title {}", URL_TO_PARSE, doc.title());
		} catch (IOException e) {
			log.error("Failed to fetch page from internet, reverting to backup method, reading saved page");
			doc = Jsoup.parse(SAVED_FILE.getFile());
			log.info("Read saved page from {}", SAVED_FILE.getPath());
		}

		// Here we assume exact structure of the page, which is not good and can break easily,
		// but it's part of parser life :)
		var tables = doc.select("table");
		// personally I don't like long chained streams after streams, so I logically split them
		var tableWithCodes = tables
				.parallelStream()
				.filter(table -> {
					var headerRows = table.select("th");
					return !headerRows.isEmpty() && headerRows.size() <= 5 && headerRows.get(0).text().equals("Serving");
				})
				.findFirst()
				.orElseThrow();

		var countryCodes = tableWithCodes
				.stream()
				.map(element -> element.select("tr"))
				// directly indexing columns so avoiding errors
				.filter(element -> !element.select("td").isEmpty() && element.select("td").size() >= 2)
				.map(this::parseElement)
				.flatMap(Collection::stream)
				.collect(Collectors.toCollection(HashSet::new));

		log.info("Parsed {} country codes entries, cleaning db and saving entries", countryCodes.size());
		service.saveToDB(countryCodes);
		;
	}

	private Collection<CountryCode> parseElement(Elements el) {
		var columns = el.select("td");
		var country = columns.get(0).text().trim();
		var code = columns.get(1).text().trim();

		// we have options here, need to construct more than one object
		if (columns.get(1).text().contains(EXTRA_START)) {
			var cutStart = code.indexOf(EXTRA_START);
			var cutEnd = code.indexOf(EXTRA_END);

			var baseCode = code.substring(0, cutStart).trim();
			return Arrays.stream(code.substring(cutStart + 1, cutEnd).split(CODE_SEPARATOR))
					.map(extraCode -> new CountryCode(country, Integer.parseInt(baseCode + extraCode.trim())))
					.collect(Collectors.toCollection(HashSet::new));
		} else if (code.contains(CODE_SEPARATOR)) {   // may also have multiple codes at once
			return Arrays.stream(code.split(CODE_SEPARATOR))
					.map(extraCode -> new CountryCode(country, Integer.parseInt(extraCode.trim())))
					.collect(Collectors.toCollection(HashSet::new));
		}

		return List.of(new CountryCode(country, Integer.parseInt(code)));
	}
}
