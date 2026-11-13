import { useState, useEffect, useRef } from 'react';
import './App.css';
import PrcConsole from './prc/PrcConsole';
import DeliveryConsole from './dlv/DeliveryConsole';

function App() {
  const restaurantCategories = {
  1: 'Other',
  2: 'Gluten-Free',
  3: 'Gluten-Free',
  4: 'Gluten-Free',
  5: 'Halal',
  6: 'Halal',
  7: 'Halal',
  8: 'Vegan',
  9: 'Vegan',
  10: 'Dairy-Free',
  11: 'Dairy-Free',
  12: 'Dairy-Free',
  13: 'Nut-Free',
  14: 'Nut-Free'
};

const categories = [
  'All',
  'Gluten-Free',
  'Halal',
  'Vegan',
  'Dairy-Free',
  'Nut-Free'
];
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [signingUp, setSigningUp] = useState(false);
  const [name, setName] = useState('');
  const [registrationPending, setRegistrationPending] = useState(false);
  const [message, setMessage] = useState('');
  const [loggedIn, setLoggedIn] = useState(false);
  const [currentUser, setCurrentUser] = useState(null);
  const [logs, setLogs] = useState([]);
  const [restaurants, setRestaurants] = useState([]);
  const [menu, setMenu] = useState([]);
  const [selectedRestaurantId, setSelectedRestaurantId] = useState(null);
  const [quantities, setQuantities] = useState({});
  const [orders, setOrders] = useState([]);
  const [activeTab, setActiveTab] = useState('restaurants');
  const [selectedCategory, setSelectedCategory] = useState('All');
  const [pendingAction, setPendingAction] = useState(null);
  const [ordersLoading, setOrdersLoading] = useState(false);
  const actionInFlight = useRef(false);
  const orderLoadInFlight = useRef(null);

    useEffect(() => {
    if (loggedIn) {
      loadRestaurants();
    }
  }, [loggedIn]);

  useEffect(() => {
    if (loggedIn && activeTab === 'orders') {
      loadRestaurantOrders();
    }
  }, [loggedIn, activeTab]);

  function addLog(message) {
    setLogs(previous => [...previous, message]);
  }

  async function getCsrfToken() {
    const response = await fetch('/login', {
      credentials: 'include'
    });

    if (!response.ok) {
      throw new Error('Cannot load login page');
    }

    const html = await response.text();
    const doc = new DOMParser().parseFromString(html, 'text/html');
    const token = doc.querySelector('input[name="_csrf"]')?.value;

    if (!token) {
      throw new Error('CSRF token not found');
    }

    return token;
  }

  async function loadRestaurants() {
    try {
      const response = await fetch('/api/restaurants', {
        credentials: 'include'
      });

      if (!response.ok) {
        throw new Error('Could not load restaurants');
      }

      const data = await response.json();
      setRestaurants(data);
      addLog('Restaurants loaded');
    } catch (error) {
      addLog('Error loading restaurants: ' + error.message);
      setMessage(error.message);
    }
  }

  async function loadMenu(restaurantId) {
    try {
      const response = await fetch(
        `/api/restaurants/${restaurantId}/menu`,
        { credentials: 'include' }
      );

      if (!response.ok) {
        throw new Error('Could not load menu');
      }

      const data = await response.json();

      setMenu(data);
      setSelectedRestaurantId(restaurantId);
      setActiveTab('menu');

      addLog('Menu loaded for restaurant ' + restaurantId);
    } catch (error) {
      addLog('Error loading menu: ' + error.message);
      setMessage(error.message);
    }
  }

  async function placeOrder(menuItemId) {
    try {
      const csrfToken = await getCsrfToken();

      const response = await fetch('/api/orders', {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          'X-CSRF-TOKEN': csrfToken
        },
        body: JSON.stringify({
          restaurantId: selectedRestaurantId,
          orderLines: [
            {
              menuItemId: menuItemId,
              quantity: Number(quantities[menuItemId] ?? 1)
            }
          ]
        })
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText);
      }

      const order = await response.json();

      addLog('Order placed successfully: #' + order.orderId);
      setMessage('Order placed successfully: #' + order.orderId);
    } catch (error) {
      addLog('Order placement failed: ' + error.message);
      setMessage('Order placement failed: ' + error.message);
    }
  }

  async function loadRestaurantOrders() {
    if (orderLoadInFlight.current) return orderLoadInFlight.current;
    const restaurantId = currentUser?.restaurantId;
    if (restaurantId == null) {
      setOrders([]);
      addLog('Error loading orders: no restaurant is assigned to this account');
      setMessage('No restaurant is assigned to this account.');
      return;
    }
    setOrdersLoading(true);
    const request = (async () => {
      try {
        const response = await fetch(`/api/restaurants/${restaurantId}/orders`, { credentials: 'include' });
        if (!response.ok) throw new Error(await response.text());
        setOrders(await response.json());
        addLog('Restaurant orders loaded');
      } catch (error) {
        addLog('Error loading orders: ' + error.message);
        setMessage(error.message);
      } finally {
        setOrdersLoading(false);
      }
    })();
    orderLoadInFlight.current = request;
    try {
      await request;
    } finally {
      if (orderLoadInFlight.current === request) orderLoadInFlight.current = null;
    }
  }


async function runOrderAction(url, action, rejectionReason = null) {
  // Prevent duplicate actions while another request is running
  if (actionInFlight.current) return;

  actionInFlight.current = true;
  setPendingAction(action);
  setMessage(action + ' in progress...');

  try {
    if (orderLoadInFlight.current) {
      await orderLoadInFlight.current;
    }

    const csrfToken = await getCsrfToken();

    const response = await fetch(url, {
      method: 'POST',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
        'X-CSRF-TOKEN': csrfToken
      },
      ...(action === 'Reject'
        ? { body: JSON.stringify({ reason: rejectionReason }) }
        : {})
    });


    if (!response.ok) {
      throw new Error(await response.text());
    }

    const order = await response.json();

   

    setOrders(previous =>
      previous.map(existing =>
        existing.orderId === order.orderId ? order : existing
      )
    );

    addLog(action + ' successful: Order #' + order.orderId);
    setMessage(action + ' successful');

    await loadRestaurantOrders();

  } catch (error) {
    addLog(action + ' failed: ' + error.message);
    setMessage(action + ' failed: ' + error.message);
  } finally {
    actionInFlight.current = false;
    setPendingAction(null);
  }
}

async function handleReject(orderId) {
  const reason = window.prompt(
    'Please enter a reason for rejecting this order:'
  );

  if (reason === null) {
    return;
  }

  if (!reason.trim()) {
    setMessage('A rejection reason is required.');
    return;
  }

  await runOrderAction(
    `/api/orders/${orderId}/reject`,
    'Reject',
    reason.trim()
  );
}

async function handleStartPreparation(order) {
  const acceptedOrders = orders
    .filter(
      o =>
        o.status === 'ACCEPTED' &&
        o.restaurantId === order.restaurantId
    )
    .sort((a, b) => a.orderId - b.orderId);

  if (acceptedOrders.length === 0) {
    setMessage('No accepted orders waiting.');
    return;
  }

  const nextOrder = acceptedOrders[0];

  if (nextOrder.orderId !== order.orderId) {
    const confirmed = window.confirm(
      `Order Queue Warning!\n\n` +
      `You selected Order #${order.orderId}, ` +
      `but Order #${nextOrder.orderId} is next in the queue.\n\n` +
      `Order #${nextOrder.orderId} needs to be prepared first.\n\n` +
      `Do you want to start Order #${nextOrder.orderId} instead?`
    );

    if (!confirmed) {
      return;
    }
  }

  await runOrderAction(
    `/api/restaurants/${order.restaurantId}/start-next`,
    'Start Preparation'
  );
}
   

  function switchAuthMode(nextSigningUp) {
    setSigningUp(nextSigningUp);
    setPassword('');
    setMessage('');
  }

  async function handleRegistration(event) {
    event.preventDefault();
    if (registrationPending) return;

    setRegistrationPending(true);
    setMessage('');
    const normalizedEmail = email.trim().toLowerCase();

    try {
      const csrfToken = await getCsrfToken();
      const response = await fetch('/api/register', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-CSRF-TOKEN': csrfToken,
        },
        credentials: 'include',
        body: JSON.stringify({ name: name.trim(), email: normalizedEmail, password }),
      });

      if (response.status === 400) {
        setMessage((await response.text()) || 'Check your registration details and try again.');
        return;
      }
      if (!response.ok) {
        setMessage('Could not create your account. Please try again.');
        return;
      }

      setEmail(normalizedEmail);
      setName('');
      switchAuthMode(false);
      setMessage('Account created. Log in with your new password.');
    } catch {
      setMessage('Could not connect to the server. Please try again.');
    } finally {
      setRegistrationPending(false);
    }
  }

  async function handleLogin(event) {
    event.preventDefault();
    setMessage('');

    try {
      const csrfToken = await getCsrfToken();

      const response = await fetch('/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        credentials: 'include',
        body: new URLSearchParams({
          username: email,
          password: password,
          _csrf: csrfToken
        })
      });

      if (response.ok) {
        const contextResponse = await fetch('/api/prc/context', { credentials: 'include' });
        if (!contextResponse.ok) throw new Error('Cannot load the authenticated user');
        setOrders([]);
        setCurrentUser(await contextResponse.json());
        setLogs([]);
        setLoggedIn(true);
        setActiveTab('restaurants');
        addLog('Login successful: ' + email);
        setMessage('Login successful');
      } else {
        addLog('Login failed: ' + email);
        setMessage('Invalid email or password');
      }
    } catch (error) {
      setMessage('Login request failed');
      console.error(error);
    }
  }

  async function handleLogout() {
    try {
      const csrfToken = await getCsrfToken();

      const response = await fetch('/logout', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        credentials: 'include',
        body: new URLSearchParams({
          _csrf: csrfToken
        })
      });

      if (response.ok) {
        addLog('Logout successful: ' + email);
        setLoggedIn(false);
        setCurrentUser(null);
        setLogs([]);
        setPassword('');
        setRestaurants([]);
        setMenu([]);
        setSelectedRestaurantId(null);
        setQuantities({});
        setOrders([]);
        setActiveTab('restaurants');
        setMessage('Logged out successfully');
      } else {
        setMessage('Logout failed');
      }
    } catch (error) {
      setMessage('Logout request failed');
      console.error(error);
    }
  }

    const filteredRestaurants = restaurants.filter(restaurant => {
    if (selectedCategory === 'All') {
      return true;
    }

    return restaurantCategories[restaurant.restaurantId] === selectedCategory;
  });

  if (loggedIn) {
    return (
      <div className="dashboard-container">
        <div className="dashboard-panel">
          <h1>CARE Eats</h1>
          <h2>Dashboard</h2>

          <p>Logged in as {email}</p>

          <nav className="dashboard-tabs">
            {['COURIER', 'OPERATOR'].includes(currentUser?.role) && (
  <button
    type="button"
    className={activeTab === 'delivery' ? 'active' : ''}
    onClick={() => setActiveTab('delivery')}
  >
    {currentUser.role === 'COURIER' ? 'Delivery' : 'Dispatch'}
  </button>
)}
            <button
              type="button"
              className={activeTab === 'restaurants' ? 'active' : ''}
              onClick={() => setActiveTab('restaurants')}
            >
              Restaurants
            </button>

            <button
              type="button"
              className={activeTab === 'menu' ? 'active' : ''}
              onClick={() => setActiveTab('menu')}
            >
              Menu
            </button>

            <button
              type="button"
              className={activeTab === 'orders' ? 'active' : ''}
              onClick={() => setActiveTab('orders')}
            >
              Restaurant Orders
            </button>

            <button
              type="button"
              className={activeTab === 'logs' ? 'active' : ''}
              onClick={() => setActiveTab('logs')}
            >
              Activity Log
            </button>
          </nav>

          
          {activeTab === 'restaurants' && (
            <section>
              <h3>Explore Restaurants</h3>

              <div className="category-tabs">
                {categories.map(category => (
                  <button
                    key={category}
                    type="button"
                    className={selectedCategory === category ? 'active' : ''}
                    onClick={() => setSelectedCategory(category)}
                  >
                    {category}
                  </button>
                ))}
              </div>

              <div className="restaurant-grid">
                {filteredRestaurants.map(restaurant => (
                  <div className="restaurant-card" key={restaurant.restaurantId}>
                    <h4>{restaurant.name}</h4>

                    <p>{restaurantCategories[restaurant.restaurantId] ?? 'Other'}</p>

                    <p>
                      Status: {restaurant.status}
                    </p>

                    <button
                      type="button"
                      onClick={() => loadMenu(restaurant.restaurantId)}
                    >
                      View Menu
                    </button>
                  </div>
                ))}
              </div>
            </section>
          )}


          {activeTab === 'menu' && (
            <section>
              <h3>Menu</h3>

              {selectedRestaurantId === null && (
                <p>Select a restaurant from the Restaurants tab.</p>
              )}

              {menu.map(item => (
                <div key={item.itemId}>
                  <p>
                    {item.name} - ${item.price} - {item.status}
                  </p>

                  {item.status === 'AVAILABLE' && (
                    <>
                      <input
                        type="number"
                        min="1"
                        step="1"
                        value={quantities[item.itemId] ?? 1}
                        onChange={event =>
                          setQuantities(previous => ({
                            ...previous,
                            [item.itemId]: event.target.value
                          }))
                        }
                      />

                      <button
                        type="button"
                        onClick={() => placeOrder(item.itemId)}
                      >
                        Place Order
                      </button>
                    </>
                  )}
                </div>
              ))}
            </section>
          )}

         {activeTab === 'orders' && (
            <section aria-busy={ordersLoading || pendingAction !== null}>
              <h3>Restaurant Orders</h3>
              {pendingAction && <p role="status">{pendingAction} in progress...</p>}

              <button
                type="button"
                disabled={ordersLoading || pendingAction !== null}
                onClick={loadRestaurantOrders}
              >
                {ordersLoading ? 'Loading orders...' : 'Refresh Orders'}
              </button>

              {orders.map(order => (
                <div key={order.orderId}>
                  <p>
                    Order #{order.orderId} — {order.status}
                  </p>

{order.status === 'PLACED' && (
  <>
    <button
      type="button"
      disabled={ordersLoading || pendingAction !== null}
      onClick={() =>
        runOrderAction(
          `/api/orders/${order.orderId}/accept`,
          'Accept'
        )
      }
    >
      Accept
    </button>

    <button
      type="button"
      disabled={ordersLoading || pendingAction !== null}
      onClick={() => handleReject(order.orderId)}
    >
      Reject
    </button>
  </>
)}

{order.status === 'ACCEPTED' && (
  <button
    type="button"
    disabled={ordersLoading || pendingAction !== null}
    onClick={() => handleStartPreparation(order)}
  >
    Start Preparation
  </button>
)}

                  {order.status === 'PREPARING' && (
                    <button
                      type="button"
                      disabled={ordersLoading || pendingAction !== null}
                      onClick={() =>
                        runOrderAction(
                          `/api/orders/${order.orderId}/ready`,
                          'Mark Ready'
                        )
                      }
                    >
                      Mark Ready
                    </button>
                  )}
                </div>
              ))}
            </section>
          )}
{activeTab === 'delivery'
  && ['COURIER', 'OPERATOR'].includes(currentUser?.role) && (
    <DeliveryConsole
      key={currentUser.userId}
      user={currentUser}
      addLog={addLog}
      getCsrfToken={getCsrfToken}
    />
)}
          {activeTab === 'logs' && currentUser && (
            <PrcConsole key={currentUser.userId} user={currentUser} logs={logs}
              addLog={addLog} getCsrfToken={getCsrfToken} />
          )}

          <p role="status">{message}</p>

          <button type="button" onClick={handleLogout}>
            Log Out
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="login-container">
      <form onSubmit={signingUp ? handleRegistration : handleLogin} className="login-form">
        <h1>CARE Eats</h1>
        <h2>{signingUp ? 'Create a customer account' : 'Login'}</h2>

        {signingUp && (
          <input
            type="text"
            aria-label="Name"
            placeholder="Name"
            autoComplete="name"
            value={name}
            onChange={event => setName(event.target.value)}
            disabled={registrationPending}
            required
          />
        )}

        <input
          type="email"
          aria-label="Email"
          placeholder="Email"
          autoComplete="username"
          value={email}
          onChange={event => setEmail(event.target.value)}
          disabled={registrationPending}
          required
        />

        <input
          type="password"
          aria-label="Password"
          placeholder="Password"
          autoComplete={signingUp ? 'new-password' : 'current-password'}
          value={password}
          onChange={event => setPassword(event.target.value)}
          disabled={registrationPending}
          required
        />

        <button type="submit" disabled={registrationPending}>
          {registrationPending ? 'Creating account…' : signingUp ? 'Sign Up' : 'Log In'}
        </button>
        <button
          type="button"
          onClick={() => switchAuthMode(!signingUp)}
          disabled={registrationPending}
        >
          {signingUp ? 'Back to Login' : 'Create a Customer Account'}
        </button>

        <p role="status">{message}</p>
      </form>
    </div>
  );
}

export default App;
