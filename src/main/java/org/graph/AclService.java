package org.graph;

import java.util.List;

/**
 * Service interface exported by ACL application.
 */
public interface AclService {

    /**
     * Gets a list containing all ACL rules.
     *
     * @return a list containing all ACL rules
     */
    List<AclRule> getAclRules();

    /**
     * Adds a new ACL rule.
     *
     * @param rule ACL rule
     * @return true if successfully added, otherwise false
     */
    boolean addAclRule(AclRule rule);

    /**
     * Removes an exsiting ACL rule by rule id.
     *
     * @param ruleId ACL rule identifier
     */
    void removeAclRule(RuleId ruleId);

    /**
     * Clears ACL and resets all.
     */
    void clearAcl();

}