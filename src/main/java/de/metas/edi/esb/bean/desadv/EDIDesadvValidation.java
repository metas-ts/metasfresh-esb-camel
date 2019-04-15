/*
 *
 *  * #%L
 *  * %%
 *  * Copyright (C) <current year> metas GmbH
 *  * %%
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as
 *  * published by the Free Software Foundation, either version 2 of the
 *  * License, or (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public
 *  * License along with this program. If not, see
 *  * <http://www.gnu.org/licenses/gpl-2.0.html>.
 *  * #L%
 *
 */

package de.metas.edi.esb.bean.desadv;

import de.metas.edi.esb.commons.Util;
import de.metas.edi.esb.jaxb.EDIExpDesadvLineType;
import de.metas.edi.esb.jaxb.EDIExpDesadvType;
import de.metas.edi.esb.route.exports.EDIDesadvRoute;
import org.apache.camel.Exchange;
import org.apache.camel.RuntimeCamelException;

import java.util.List;

import static de.metas.edi.esb.commons.ValidationHelper.validateObject;
import static de.metas.edi.esb.commons.ValidationHelper.validateString;

public class EDIDesadvValidation
{
	public EDIExpDesadvType validateExchange(final Exchange exchange)
	{
		// validate mandatory exchange properties
		validateString(exchange.getProperty(EDIDesadvRoute.EDI_DESADV_IS_TEST, String.class), "exchange property IsTest cannot be null or empty");

		final EDIExpDesadvType xmlDesadv = exchange.getIn().getBody(EDIExpDesadvType.class);
		validateObject(xmlDesadv, "@EDI.DESADV.XmlNotNull@"); // DESADV body shall not be null

		// validate mandatory types (not null)
		validateObject(xmlDesadv.getCBPartnerID(), "@FillMandatory@ @EDI_DESADV_ID@=" + xmlDesadv.getDocumentNo() + " @C_BPartner_ID@");
		validateObject(xmlDesadv.getBillLocationID(), "@FillMandatory@ @EDI_DESADV_ID@=" + xmlDesadv.getDocumentNo() + " @Bill_Location_ID@");
		validateObject(xmlDesadv.getSequenceNoAttr(), "@FillMandatory@ @EDI_DESADV_ID@=" + xmlDesadv.getDocumentNo() + " @SequenceNoAttr@");
		validateObject(xmlDesadv.getMovementDate(), "@FillMandatory@ @EDI_DESADV_ID@=" + xmlDesadv.getDocumentNo() + " @MovementDate@");
		validateObject(xmlDesadv.getDateOrdered(), "@FillMandatory@ @EDI_DESADV_ID@=" + xmlDesadv.getDocumentNo() + " @DateOrdered@");
		validateObject(xmlDesadv.getCBPartnerLocationID(), "@FillMandatory@ @EDI_DESADV_ID@=" + xmlDesadv.getDocumentNo() + " @C_BPartner_Location_ID@");

		validateObject(xmlDesadv.getCCurrencyID(), "@FillMandatory@ @EDI_DESADV_ID@=" + xmlDesadv.getDocumentNo() + " @C_Currency_ID@");

		// validate strings (not null or empty)
		validateString(xmlDesadv.getCBPartnerID().getEdiRecipientGLN(), "@FillMandatory@ @C_BPartner_ID@=" + xmlDesadv.getCBPartnerID().getValue() + " @EdiRecipientGLN@");
		validateString(xmlDesadv.getCBPartnerLocationID().getGLN(), "@FillMandatory@ @C_BPartner_Location_ID@ @EDI_DESADV_ID@=" + xmlDesadv.getDocumentNo() + " @GLN@");
		validateObject(xmlDesadv.getBillLocationID().getGLN(), "@FillMandatory@ @Bill_Location_ID@ @EDI_DESADV_ID@=" + xmlDesadv.getDocumentNo() + " @GLN@");

		// Evaluate EDI_DesadvLines
		final List<EDIExpDesadvLineType> ediExpDesadvLines = xmlDesadv.getEDIExpDesadvLine();
		if (ediExpDesadvLines.isEmpty())
		{
			throw new RuntimeCamelException("@EDI.DESADV.ContainsDesadvLines@");
		}

		for (final EDIExpDesadvLineType xmlDesadvLine : ediExpDesadvLines)
		{
			// validate mandatory types (not null)
			validateObject(xmlDesadvLine.getLine(), "@FillMandatory@  @EDI_DesadvLine_ID@ @Line@");

			validateObject(xmlDesadvLine.getQtyDeliveredInUOM(), "@FillMandatory@ @EDI_DesadvLine_ID@=" + xmlDesadvLine.getLine() + " @QtyDeliveredInUOM@");
			validateObject(xmlDesadvLine.getCUOMID(), "@FillMandatory@ @EDI_DesadvLine_ID@=" + xmlDesadvLine.getLine() + " @C_UOM_ID@");
			validateObject(xmlDesadvLine.getMProductID(), "@FillMandatory@ @EDI_DesadvLine_ID@=" + xmlDesadvLine.getLine() + " @M_Product_ID@");

			validateObject(xmlDesadvLine.getQtyItemCapacity(), "@FillMandatory@ @EDI_DesadvLine_ID@=" + xmlDesadvLine.getLine() + " @QtyItemCapacity@");
			validateObject(xmlDesadvLine.getQtyEntered(), "@FillMandatory@ @EDI_DesadvLine_ID@=" + xmlDesadvLine.getLine() + " @QtyEntered@");

			if (xmlDesadvLine.getQtyDeliveredInUOM().signum() > 0) // if it's a P102-line, then there can be no SSCC18 and that's OK
			{
				final String sscc18Value = Util.removePrecedingZeros(xmlDesadvLine.getIPASSCC18());
				validateString(sscc18Value, "@FillMandatory@ SSCC18 in @EDI_DesadvLine_ID@ " + xmlDesadvLine.getLine());
			}

			validateString(xmlDesadvLine.getProductNo(), "@FillMandatory@ ProductNo in @EDI_DesadvLine_ID@ " + xmlDesadvLine.getLine());
			validateString(xmlDesadvLine.getUPC(), "@FillMandatory@ UPC in @EDI_DesadvLine_ID@ " + xmlDesadvLine.getLine());
			validateString(xmlDesadvLine.getProductDescription(), "@FillMandatory@ ProductDescription in @EDI_DesadvLine_ID@ " + xmlDesadvLine.getLine());
		}

		return xmlDesadv;
	}
}