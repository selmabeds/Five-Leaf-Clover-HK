import { useState, useEffect, useRef } from 'react';
import './App.css';
import PrcConsole from './prc/PrcConsole';
import DeliveryConsole from './dlv/DeliveryConsole';
import LoginView from './dash/components/LoginView';
import MenuView from './dash/components/MenuView';
import RestaurantOrderView from './dash/components/RestaurantOrderView';
import {
  getCsrfToken,
  login,
  getCurrentUser,
  logout,
  register
} from './dash/services/authService';
import {
  getRestaurants,
  getMenu,
  placeOrder,
  getRestaurantOrders,
  acceptOrder,
  rejectOrder,
  startPreparation,
  markOrderReady
} from './dash/services/orderService';

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
  // In-page pop-up (browser pop-ups like window.alert can be blocked)
  const [notice, setNotice] = useState(null);
  // Order currently being rejected, and the reason typed for it
  const [rejectingOrderId, setRejectingOrderId] = useState(null);
  const [rejectionReason, setRejectionReason] = useState('');
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

  async function loadRestaurants() {
    try {
      const data = await getRestaurants();
      setRestaurants(data);
      addLog('Restaurants loaded');
    } catch (error) {
      addLog('Error loading restaurants: ' + error.message);
      setMessage(error.message);
    }
  }

  async function loadMenu(restaurantId) {
    try {
      const data = await getMenu(restaurantId);

      setMenu(data);
      setSelectedRestaurantId(restaurantId);
      setActiveTab('menu');

      addLog('Menu loaded for restaurant ' + restaurantId);
    } catch (error) {
      addLog('Error loading menu: ' + error.message);
      setMessage(error.message);
    }
  }

  async function handlePlaceOrder(menuItemId) {
    try {
      const csrfToken = await getCsrfToken();

      const order = await placeOrder({
        restaurantId: selectedRestaurantId,
        orderLines: [
          {
            menuItemId: menuItemId,
            quantity: Number(quantities[menuItemId] ?? 1)
          }
        ]
      }, csrfToken);

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
        const data = await getRestaurantOrders(restaurantId);
        setOrders(data);
        addLog('Restaurant orders loaded for restaurant ' + restaurantId + ' (' + data.length + ' orders)');
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


// Runs one restaurant-order command; `command` is an orderService.js call that receives the CSRF token
async function runOrderAction(action, command) {
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
    const order = await command(csrfToken);

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

function startReject(orderId) {
  setRejectingOrderId(orderId);
  setRejectionReason('');
}

function cancelReject() {
  setRejectingOrderId(null);
  setRejectionReason('');
}

async function handleReject(orderId) {
  const reason = rejectionReason.trim();

  if (!reason) {
    addLog('Reject failed: a rejection reason is required');
    setMessage('A rejection reason is required.');
    return;
  }

  cancelReject();

  await runOrderAction('Reject', csrfToken => rejectOrder(orderId, reason, csrfToken));
}

function openMenuTab() {
  if (selectedRestaurantId === null) {
    addLog('Menu: no restaurant selected');
    setNotice('Please select a restaurant first to view its menu.');
    return;
  }
  setActiveTab('menu');
}

function closeNotice() {
  setNotice(null);
  setActiveTab('restaurants');
}

// Only shown on the order whose turn it is (RestaurantOrderView decides), so no pop-up is needed
async function handleStartPreparation(order) {
  await runOrderAction('Start Preparation', csrfToken => startPreparation(order.restaurantId, csrfToken));
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
      const response = await register({ name: name.trim(), email: normalizedEmail, password });

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
      const loginAccepted = await login({ email, password });

      if (loginAccepted) {
        const user = await getCurrentUser();
        setOrders([]);
        setCurrentUser(user);
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
      const loggedOut = await logout();

      if (loggedOut) {
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

  const selectedRestaurant = restaurants.find(
    restaurant => restaurant.restaurantId === selectedRestaurantId
  );

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
              onClick={openMenuTab}
            >
              {selectedRestaurant && activeTab !== 'menu'
                ? 'View previous menu: ' + selectedRestaurant.name
                : 'Menu'}
            </button>

            {currentUser?.role === 'RESTAURANT' && (
              <button
                type="button"
                className={activeTab === 'orders' ? 'active' : ''}
                onClick={() => setActiveTab('orders')}
              >
                Restaurant Orders
              </button>
            )}

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
            <MenuView
              selectedRestaurant={selectedRestaurant}
              selectedRestaurantId={selectedRestaurantId}
              menu={menu}
              quantities={quantities}
              onQuantityChange={(itemId, value) =>
                setQuantities(previous => ({ ...previous, [itemId]: value }))}
              onPlaceOrder={handlePlaceOrder}
            />
          )}

         {activeTab === 'orders' && currentUser?.role === 'RESTAURANT' && (
            <RestaurantOrderView
              orders={orders}
              ordersLoading={ordersLoading}
              pendingAction={pendingAction}
              rejectingOrderId={rejectingOrderId}
              rejectionReason={rejectionReason}
              onRejectionReasonChange={setRejectionReason}
              onRefresh={loadRestaurantOrders}
              onAccept={orderId =>
                runOrderAction('Accept', csrfToken => acceptOrder(orderId, csrfToken))}
              onStartReject={startReject}
              onConfirmReject={handleReject}
              onCancelReject={cancelReject}
              onStartPreparation={handleStartPreparation}
              onMarkReady={orderId =>
                runOrderAction('Mark Ready', csrfToken => markOrderReady(orderId, csrfToken))}
            />
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

          {notice && (
            <div className="notice-backdrop">
              <div className="notice-box" role="alertdialog" aria-modal="true" aria-label="Notice">
                <p>{notice}</p>
                <button type="button" onClick={closeNotice} autoFocus>
                  OK
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    );
  }

  return (
    <LoginView
      signingUp={signingUp}
      name={name}
      email={email}
      password={password}
      registrationPending={registrationPending}
      message={message}
      onNameChange={setName}
      onEmailChange={setEmail}
      onPasswordChange={setPassword}
      onSubmit={signingUp ? handleRegistration : handleLogin}
      onToggleMode={() => switchAuthMode(!signingUp)}
    />
  );
}

export default App;
