/**
 * VStar: a statistical analysis tool for variable star data.
 * Copyright (C) 2010  AAVSO (http://www.aavso.org/)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */
package org.aavso.tools.vstar.input.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.aavso.tools.vstar.exception.AuthenticationError;
import org.aavso.tools.vstar.exception.ConnectionException;
import org.aavso.tools.vstar.ui.resources.LoginType;
import org.aavso.tools.vstar.ui.resources.ResourceAccessor;

// TODO: refactor with CS class

/**
 * This class authenticates with respect to an AAVSO source.
 */
public class AAVSOAuthenticationSource implements IAuthenticationSource {

	private AAVSODatabaseConnector connector;
	private PreparedStatement authStmt;
	private PreparedStatement obsCodeStmt;
	private boolean authenticated;

	public AAVSOAuthenticationSource() {
		this.connector = AAVSODatabaseConnector.aavsoUserDBConnector;
		this.authenticated = false;
	}

	@Override
	public boolean authenticate(String username, String password)
			throws AuthenticationError, ConnectionException {
		if (!authenticated) {
			try {
				// Get a prepared statement to read a user's details
				// from the database, setting the parameters for user
				// who is authenticating.
				PreparedStatement userStmt = createLoginQuery(connector
						.getConnection());
				userStmt.setString(1, username);

				String passwordDigest = Authenticator
						.generateHexDigest(password);

				ResultSet userResults = userStmt.executeQuery();

				if (userResults.next()) {
					String actualPassword = userResults.getString("pass");
					if (passwordDigest.equals(actualPassword)) {
						// We're authenticated, so update login info and
						// retrieve observer code if the user has one.
						authenticated = true;

						ResourceAccessor.getLoginInfo().setType(getLoginType());
						ResourceAccessor.getLoginInfo().setUserName(username);

						retrieveObserverCode(username);
					}
				}
			} catch (SQLException e) {
				throw new AuthenticationError(e.getLocalizedMessage());
			}
		}

		return authenticated;
	}

	@Override
	public LoginType getLoginType() {
		return LoginType.AAVSO;
	}

	/**
	 * Retrieve the observer code given the user name, if we are authenticated,
	 * otherwise return null.
	 */
	private String retrieveObserverCode(String username) {
		String obsCode = null;

		if (authenticated) {
			try {
				PreparedStatement obsCodeStmt = createObserverCodeQuery(connector
						.getConnection());
				obsCodeStmt.setString(1, username);
				ResultSet obsCodeResults = obsCodeStmt.executeQuery();

				if (obsCodeResults.next()) {
					obsCode = obsCodeResults.getString("value");
					if (!obsCodeResults.wasNull()) {
						ResourceAccessor.getLoginInfo()
								.setObserverCode(obsCode);
					}
				}
			} catch (SQLException e) {
				// Nothing to do: just return null.
			} catch (ConnectionException e) {
				// Nothing to do: just return null.
			}
		}

		return obsCode;
	}

	/**
	 * Return a prepared statement for the specified AAVSO user login.
	 * 
	 * This is a once-only-created prepared statement with parameters set for
	 * each query execution.
	 * 
	 * @param connection
	 *            database connection.
	 * @return A prepared statement.
	 */
	private PreparedStatement createLoginQuery(Connection connection)
			throws SQLException {
		if (authStmt == null) {
			authStmt = connection
					.prepareStatement("SELECT users.name, users.pass, users.uid "
							+ "FROM users WHERE users.name = ?");
		}

		return authStmt;
	}

	/**
	 * Return a prepared statement to obtain the observer code given the AAVSO
	 * user's user ID.
	 * 
	 * This is a once-only-created prepared statement with parameters set for
	 * each query execution.
	 * 
	 * @param connection
	 *            database connection.
	 * @return A prepared statement.
	 */
	private PreparedStatement createObserverCodeQuery(Connection connection)
			throws SQLException {
		if (obsCodeStmt == null) {
			obsCodeStmt = connection
					.prepareStatement("SELECT value from users, profile_values "
							+ "WHERE profile_values.fid = 21 AND "
							+ "profile_values.uid = users.uid AND users.name = ?;");
		}

		return obsCodeStmt;
	}
}
