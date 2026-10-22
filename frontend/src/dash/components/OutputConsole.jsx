
export default function OutputConsole({ messages = [] }) {
  return (
    <div>
      <h2>Output Console</h2>
      <ul>
        {messages.map((message, index) => (
          <li key={index}>{message}</li>
        ))}
      </ul>
    </div>
  );
}
