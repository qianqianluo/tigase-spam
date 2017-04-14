/*
 * SpamProcessor.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
package tigase.spam;

import tigase.db.NonAuthUserRepository;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.RegistrarBean;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.core.Kernel;
import tigase.server.Packet;
import tigase.server.xmppsession.SessionManager;
import tigase.spam.filters.KnownSpammersFilter;
import tigase.stats.StatisticsList;
import tigase.xmpp.XMPPPreprocessorIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.impl.annotation.AnnotatedXMPPProcessor;
import tigase.xmpp.impl.annotation.Id;

import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.spam.SpamProcessor.ID;

/**
 * Created by andrzej on 08.04.2017.
 */
@Id(ID)
@Bean(name = ID, parent = SessionManager.class, active = false)
public class SpamProcessor
		extends AnnotatedXMPPProcessor
		implements XMPPPreprocessorIfc, RegistrarBean {

	protected static final String ID = "spam-filter";

	private static final Logger log = Logger.getLogger(SpamProcessor.class.getCanonicalName());

	@Inject
	private CopyOnWriteArrayList<SpamFilter> filters = new CopyOnWriteArrayList<>();

	@Inject
	private CopyOnWriteArrayList<ResultsAwareSpamFilter> resultsAwareFilters = new CopyOnWriteArrayList<>();

	@ConfigField(desc = "Return error if packet is dropped", alias = "return-error")
	private boolean returnError = false;

	@Override
	public boolean preProcess(Packet packet, XMPPResourceConnection session,
							  NonAuthUserRepository nonAuthUserRepository, Queue<Packet> queue,
							  Map<String, Object> map) {
		for (SpamFilter filter : filters) {
			if (!filter.filter(packet, session)) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "filter {0} detected spam message {1}, sending error = {2}",
							new Object[]{filter.getId(), packet, returnError});
				}
				resultsAwareFilters.forEach(resultAware -> resultAware.identifiedSpam(packet, session));
				if (!returnError) {
					packet.processedBy(ID);
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public void register(Kernel kernel) {
		
	}

	@Override
	public void unregister(Kernel kernel) {

	}

	@Override
	public void getStatistics(StatisticsList list) {
		super.getStatistics(list);
		filters.forEach(filter -> filter.getStatistics(this.id(), list));
	}

	public void setFilters(CopyOnWriteArrayList<SpamFilter> filters) {
		if (filters == null) {
			filters = new CopyOnWriteArrayList<>();
		}
		KnownSpammersFilter knownSpammers = null;
		Iterator<SpamFilter> it = filters.iterator();
		while (it.hasNext()) {
			SpamFilter filter = it.next();
			if (filter instanceof KnownSpammersFilter) {
				knownSpammers = (KnownSpammersFilter) filter;
				it.remove();
				break;
			}
		}
		if (knownSpammers != null) {
			filters.add(0, knownSpammers);
		}
		this.filters = filters;
	}

	public void setResultsAwareFilters(CopyOnWriteArrayList<ResultsAwareSpamFilter> resultsAwareFilters) {
		if (resultsAwareFilters == null) {
			resultsAwareFilters = new CopyOnWriteArrayList<>();
		}
		this.resultsAwareFilters = resultsAwareFilters;
	}
}
