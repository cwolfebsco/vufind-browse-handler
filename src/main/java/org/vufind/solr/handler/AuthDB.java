package org.vufind.solr.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.solr.search.SolrIndexSearcher;

/**
 *
 * Interface to the Solr Authority DB
 *
 */
public class AuthDB
{
    static int MAX_PREFERRED_HEADINGS = 1000;

    private SolrIndexSearcher searcher;
    private String preferredHeadingField;
    private String useInsteadHeadingField;
    private String seeAlsoHeadingField;
    private String broaderHeadingField;
    private String narrowerHeadingField;
    private String scopeNoteField;

    public AuthDB(SolrIndexSearcher authSearcher,
                  String preferredField,
                  String useInsteadField,
                  String seeAlsoField,
                  String broaderField,
                  String narrowerField,
                  String noteField)
    {
        searcher = authSearcher;
        preferredHeadingField = preferredField;
        useInsteadHeadingField = useInsteadField;
        seeAlsoHeadingField = seeAlsoField;
        broaderHeadingField = broaderField;
        narrowerHeadingField = narrowerField;
        scopeNoteField = noteField;
    }


    private List<String> docValues(Document doc, String field)
    {
        String values[] = doc.getValues(field);

        if (values == null) {
            values = new String[] {};
        }

        return Arrays.asList(values);
    }


    public Document getAuthorityRecord(String heading)
    throws Exception
    {
        TopDocs results = (searcher.search(new TermQuery(new Term(preferredHeadingField,
                                           heading)),
                                           1));

        if (results.totalHits.value > 0) {
            return searcher.getIndexReader().storedFields().document(results.scoreDocs[0].doc);
        } else {
            return null;
        }
    }


    public List<Document> getPreferredHeadings(String heading)
    throws Exception
    {
        TopDocs results = (searcher.search(new TermQuery(new Term(useInsteadHeadingField,
                                           heading)),
                                           MAX_PREFERRED_HEADINGS));
        List<Document> result = new ArrayList<> ();

        StoredFields storedFields = searcher.getIndexReader().storedFields();
        for (int i = 0; i < results.totalHits.value; i++) {
            result.add(storedFields.document(results.scoreDocs[i].doc));
        }

        return result;
    }


    public Map<String, List<String>> getFields(String heading)
    throws Exception
    {
        Document authInfo = getAuthorityRecord(heading);

        Map<String, List<String>> itemValues = new HashMap<> ();

        itemValues.put("seeAlso", new ArrayList<String>());
        itemValues.put("useInstead", new ArrayList<String>());
        itemValues.put("broader", new ArrayList<String>());
        itemValues.put("narrower", new ArrayList<String>());
        itemValues.put("note", new ArrayList<String>());

        if (authInfo != null) {
            for (String value : docValues(authInfo, seeAlsoHeadingField)) {
                itemValues.get("seeAlso").add(value);
            }

            for (String value : docValues(authInfo, broaderHeadingField)) {
                itemValues.get("broader").add(value);
            }
            
            for (String value : docValues(authInfo, narrowerHeadingField)) {
                itemValues.get("narrower").add(value);
            }

            for (String value : docValues(authInfo, scopeNoteField)) {
                itemValues.get("note").add(value);
            }
        } else {
            List<Document> preferredHeadings =
                getPreferredHeadings(heading);

            for (Document doc : preferredHeadings) {
                for (String value : docValues(doc, preferredHeadingField)) {
                    itemValues.get("useInstead").add(value);
                }

                for (String value : docValues(doc, broaderHeadingField)) {
                    itemValues.get("broader").add(value);
                }
                
                for (String value : docValues(doc, narrowerHeadingField)) {
                    itemValues.get("narrower").add(value);
                }
            }
        }

        return itemValues;
    }
}
