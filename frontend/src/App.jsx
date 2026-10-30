import { useState, useEffect } from 'react';
import './App.css';

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
  const [message, setMessage] = useState('');
  const [loggedIn, setLoggedIn] = useState(false);
  const [logs, setLogs] = useState([]);
  const [restaurants, setRestaurants] = useState([]);
  const [menu, setMenu] = useState([]);
  const [selectedRestaurantId, setSelectedRestaurantId] = useState(null);
  const [quantities, setQuantities] = useState({});
  const [orders, setOrders] = useState([]);
  const [activeTab, setActiveTab] = useState('restaurants');
  const [selectedCategory, setSelectedCategory] = useState('All');

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
    try {
      const response = await fetch('/api/restaurants/1/orders', {
        credentials: 'include'
      });

      if (!response.ok) {
        throw new Error(await response.text());
      }

      const data = await response.json();
      setOrders(data);
      addLog('Restaurant orders loaded');
    } catch (error) {
      addLog('Error loading orders: ' + error.message);
      setMessage(error.message);
    }
  }

  async function runOrderAction(url, action) {
    try {
      const csrfToken = await getCsrfToken();

      const response = await fetch(url, {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          'X-CSRF-TOKEN': csrfToken
        },
        ...(action === 'Reject'
          ? { body: JSON.stringify({ reason: 'Rejected by restaurant' }) }
          : {})
      });

      if (!response.ok) {
        throw new Error(await response.text());
      }

      const order = await response.json();

      addLog(action + ' successful: Order #' + order.orderId);
      setMessage(action + ' successful');

      await loadRestaurantOrders();
    } catch (error) {
      addLog(action + ' failed: ' + error.message);
      setMessage(action + ' failed: ' + error.message);
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
            <section>
              <h3>Restaurant Orders</h3>

              <button
                type="button"
                onClick={loadRestaurantOrders}
              >
                Refresh Orders
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
                        onClick={() =>
                          runOrderAction(
                            `/api/orders/${order.orderId}/reject`,
                            'Reject'
                          )
                        }
                      >
                        Reject
                      </button>
                    </>
                  )}

                  {order.status === 'ACCEPTED' && (
                    <button
                      type="button"
                      onClick={() =>
                        runOrderAction(
                          `/api/restaurants/${order.restaurantId}/start-next`,
                          'Start Preparation'
                        )
                      }
                    >
                      Start Preparation
                    </button>
                  )}

                  {order.status === 'PREPARING' && (
                    <button
                      type="button"
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

          {activeTab === 'logs' && (
            <section>
              <h3>Activity Log</h3>

              <div className="activity-log">
                {logs.map((log, index) => (
                  <p key={index}>{log}</p>
                ))}
              </div>
            </section>
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
      <form onSubmit={handleLogin} className="login-form">
        <h1>CARE Eats</h1>
        <h2>Login</h2>

        <input
          type="email"
          placeholder="Email"
          value={email}
          onChange={event => setEmail(event.target.value)}
          required
        />

        <input
          type="password"
          placeholder="Password"
          value={password}
          onChange={event => setPassword(event.target.value)}
          required
        />

        <button type="submit">Log In</button>

        <p role="status">{message}</p>
      </form>
    </div>
  );
}

export default App;
