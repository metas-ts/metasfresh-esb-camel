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

package de.metas.edi.esb.bean.order;

import de.metas.edi.esb.pojo.common.MeasurementUnit;
import de.metas.edi.esb.pojo.order.*;
import de.metas.edi.esb.pojo.order.compudata.H000;
import de.metas.edi.esb.pojo.order.compudata.H100;
import de.metas.edi.esb.pojo.order.compudata.P100;
import de.metas.edi.esb.pojo.order.qualifier.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class XMLEDIOrdersBean extends AbstractEDIOrdersBean
{

	@Override protected List<OrderEDI> getEDIDocumentObjects(List<Object> orders)
	{
		final List<OrderEDI> ediDocuments = new ArrayList<>();

		final OrderEDI orderEDI = new OrderEDI(new H000());
		ediDocuments.add(orderEDI);
		Document ordersDocument = (Document)orders.get(0);
		for (final Xbest4H order : ordersDocument.getXbest4H())
		{
			final HEADERXbest header = order.getHEADER();
			H100 h100 = mapToH100(header);

			final OrderHeader orderHeader = new OrderHeader(h100);
			orderEDI.addOrderHeader(orderHeader);
			for (final DETAILXbest detail : header.getDETAIL())
			{

				final P100 p100 = mapToP100(header, detail);

				final OrderLine orderLine = new OrderLine(p100);
				orderHeader.addOrderLine(orderLine);

			}
		}

		return ediDocuments;
	}

	private P100 mapToP100(HEADERXbest header, DETAILXbest detail)
	{
		final P100 p100 = new P100();
		p100.setPositionNo(detail.getLINENUMBER());

		BigDecimal cutuQty = BigDecimal.ZERO;
		BigDecimal orderQty = BigDecimal.ZERO;
		String orderUnit = StringUtils.EMPTY;
		for (final DQUAN1 dquan1 : detail.getDQUAN1())
		{
			final QuantityQual quantityQual = QuantityQual.valueOf(dquan1.getQUANTITYQUAL());
			switch (quantityQual)
			{
				case CUTU:
				{
					final String quantityStr = dquan1.getQUANTITY();
					cutuQty = cutuQty.add(new BigDecimal(quantityStr));
					break;
				}
				case ORDR:
				{
					final String quantityStr = dquan1.getQUANTITY();
					orderQty = orderQty.add(new BigDecimal(quantityStr));
					//using measurement unit from ORDR, supposing CUTU will not have measurement unit for now
					MeasurementUnit measurementUnit = MeasurementUnit.valueOf(dquan1.getMEASUREMENTUNIT());
					if (measurementUnit != null)
					{
						orderUnit = measurementUnit.getCuom();
					}
					break;
				}
			}
		}
		if (cutuQty.compareTo(BigDecimal.ZERO) == 0)
		{
			cutuQty = BigDecimal.ONE;
		}
		p100.setCuperTU(cutuQty.toString());
		p100.setOrderQty(orderQty.toString());
		p100.setOrderUnit(orderUnit);

		//using only first price
		BigDecimal price = BigDecimal.ZERO;
		if (!CollectionUtils.isEmpty(detail.getDPRIC1()))
		{
			price = new BigDecimal(detail.getDPRIC1().get(0).getPRICE());
		}
		p100.setBuyerPrice(price.toString());

		// ProductDescription
		for (final DPRDE1 dprde1 : detail.getDPRDE1())
		{
			if (ProductDescQual.valueOf(dprde1.getPRODUCTDESCQUAL()) == ProductDescQual.PROD)
			{
				final String productDescription = dprde1.getPRODUCTDESCTEXT();
				p100.setArtDescription(productDescription);
				break;
			}
		}

		// Product information
		for (DPRIN1 dprin1 : detail.getDPRIN1())
		{
			final ProductQual productQual = ProductQual.valueOf(dprin1.getPRODUCTQUAL());
			if (productQual == ProductQual.EANT)
			{
				p100.setEanArtNo(dprin1.getPRODUCTID());
				break;
			}
			if (productQual == ProductQual.UPCT)
			{
				p100.setEanArtNo(dprin1.getPRODUCTID());
				break;
			}
			if (productQual == ProductQual.EANC)
			{
				p100.setEanArtNo(dprin1.getPRODUCTID());
				break;
			}
			if (productQual == ProductQual.UPCC)
			{
				p100.setEanArtNo(dprin1.getPRODUCTID());
				break;
			}
			if (productQual == ProductQual.GTIN)
			{
				p100.setEanArtNo(dprin1.getPRODUCTID());
				break;
			}
		}

		if (header.getHCURR1() != null)
		{
			final String currency = header.getHCURR1().getCURRENCYCODE();
			p100.setCurrency(currency);
		}
		return p100;
	}

	private H100 mapToH100(HEADERXbest header)
	{
		H100 h100 = new H100();

		for (final HADRE1 hadre1 : header.getHADRE1())
		{
			final AddressQual addressQual = AddressQual.valueOf(hadre1.getADDRESSQUAL());
			switch (addressQual)
			{
				case SUPL:
				{
					h100.setSupplierID(hadre1.getPARTYIDGLN());
					break;
				}
				case BUYR:
				{
					h100.setBuyerID(hadre1.getPARTYIDGLN());
					break;
				}
				case DELV:
				{
					h100.setDeliveryID(hadre1.getPARTYIDGLN());
					break;
				}
				case IVCE:
				{
					h100.setInvoiceID(hadre1.getPARTYIDGLN());
					break;
				}
				case ULCO:
				{
					h100.setStoreNumber(hadre1.getPARTYIDGLN());
					break;
				}
			}
		}

		for (final HDATE1 hdate1 : header.getHDATE1())
		{
			final DateQual dateQual = DateQual.valueOf(hdate1.getDATEQUAL());
			switch (dateQual)
			{
				case CREA:
				{
					h100.setMessageDate(hdate1.getDATEFROM());
					break;
				}
				case DELV:
				{
					if (hdate1.getDATEFROM() == null)
					{
						h100.setDeliveryDate(hdate1.getDATETO());
					}
					else
					{
						h100.setDeliveryDate(hdate1.getDATEFROM());
					}
					break;
				}
			}
		}
		h100.setDocumentNo(header.getDOCUMENTID());
		return h100;
	}

}