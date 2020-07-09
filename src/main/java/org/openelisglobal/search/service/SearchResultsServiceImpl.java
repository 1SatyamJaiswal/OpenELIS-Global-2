package org.openelisglobal.search.service;

import java.util.List;

import org.openelisglobal.common.provider.query.PatientSearchResults;
import org.openelisglobal.sample.dao.SearchResultsDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SearchResultsServiceImpl implements SearchResultsService {

    @Autowired
    SearchResultsDAO searchResultsDAO;

    @Override
    @Transactional
    public List<PatientSearchResults> getSearchResults(String lastName, String firstName, String STNumber,
            String subjectNumber, String nationalID, String externalID, String patientID, String guid, String dateOfBirth) {
        return searchResultsDAO.getSearchResults(lastName, firstName, STNumber, subjectNumber, nationalID, externalID,
                patientID, guid, dateOfBirth);
    }

}
