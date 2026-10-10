# Mission Fallback Contract

Every wake mission has a deterministic fallback.

Rules:
1. Technical failure never permanently blocks a wake session.
2. Fallback does not require AI.
3. Fallback does not require network.
4. User can manually switch mission from the wake screen.
5. Emergency Stop is always available.
6. A fallback is recorded in the wake session.
7. Technical failures are not treated as user failures.

Default chains:
- Photo → QR → Math
- Steps → QR → Math
- QR → Math
- Mixed → QR → Math
- Math → no mission fallback; retry/emergency controls remain available.
