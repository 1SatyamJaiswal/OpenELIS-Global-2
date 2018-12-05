/**
* The contents of this file are subject to the Mozilla Public License
* Version 1.1 (the "License"); you may not use this file except in
* compliance with the License. You may obtain a copy of the License at
* http://www.mozilla.org/MPL/
*
* Software distributed under the License is distributed on an "AS IS"
* basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
* License for the specific language governing rights and limitations under
* the License.
*
* The Original Code is OpenELIS code.
*
* Copyright (C) CIRG, University of Washington, Seattle WA.  All Rights Reserved.
*
*/
package us.mn.state.health.lims.referral.valueholder;

import us.mn.state.health.lims.common.valueholder.BaseObject;

public class ReferralType extends BaseObject {

	private static final long serialVersionUID = 1L;

	private String id;
	private String name;
	private String description;
	private String displayKey;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getDisplayKey() {
		return displayKey;
	}
	public void setDisplayKey(String displayKey) {
		this.displayKey = displayKey;
	}

	@Override
	public String getDefaultLocalizedName(){
		return name;
	}
}
